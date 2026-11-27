import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, statfs, writeFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import os from 'node:os';
import path from 'node:path';

const [engine, binary, app] = process.argv.slice(2);
assert.ok(['chrome', 'edge'].includes(engine));
if (app) assert.equal(new URL(app).hostname, '127.0.0.1');
const qa = path.resolve('docs/qa/BIZ-UX-03');
const scratch = path.resolve('target/biz-ux-03', 'browser-' + engine + '-' + Date.now());
const profile = path.join(scratch, 'profile');
const cache = path.join(scratch, 'cache');
for (const dir of [profile, cache]) {
  await mkdir(dir, { recursive: true });
  await writeFile(path.join(dir, 'qa-write-probe'), 'BIZ-UX-03', { flag: 'wx' });
}
const disk = await statfs(scratch);
const evidence = {
  engine, separateProfileAndDiskCache: true,
  processResources: {
    freeMemoryMiB: Math.round(os.freemem() / 2 ** 20),
    totalMemoryMiB: Math.round(os.totalmem() / 2 ** 20),
    availableDiskMiB: Math.round(disk.bavail * disk.bsize / 2 ** 20),
  },
  steps: [],
};
const record = (step, result, detail) => evidence.steps.push({ step, result, detail });
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const server = createServer((req, res) => {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.end('<!doctype html><html lang="vi"><title>BeautyShop probe</title><label for="q">Tên sản phẩm</label><input id="q"><button id="send">Tìm</button><output></output><script>document.querySelector("#send").onclick=()=>document.querySelector("output").value=document.querySelector("#q").value</script>');
});
await new Promise((resolve, reject) => server.once('error', reject).listen(0, '127.0.0.1', resolve));
const probeUrl = 'http://127.0.0.1:' + server.address().port + '/';
const child = spawn(binary, [
  '--headless', '--no-first-run', '--no-default-browser-check', '--disable-background-networking',
  '--enable-logging=stderr', '--remote-debugging-address=127.0.0.1', '--remote-debugging-port=0',
  '--user-data-dir=' + profile, '--disk-cache-dir=' + cache, probeUrl,
], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
const sockets = [];
const stderr = [];
child.stderr.on('data', chunk => stderr.push(chunk.toString()));
child.on('error', error => { evidence.launchError = error.code; });

async function connect(url) {
  const ws = new WebSocket(url);
  sockets.push(ws);
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(Error('WebSocket connection timeout')), 4000);
    ws.addEventListener('open', () => { clearTimeout(timer); resolve(); }, { once: true });
    ws.addEventListener('error', () => { clearTimeout(timer); reject(Error('WebSocket connection failed')); }, { once: true });
  });
  let sequence = 0;
  const pending = new Map();
  const events = [];
  ws.addEventListener('message', ({ data }) => {
    const message = JSON.parse(data);
    if (message.id) pending.get(message.id)?.(message);
    else events.push(message);
  });
  return {
    events,
    send(method, params = {}, sessionId) {
      const id = ++sequence;
      return new Promise((resolve, reject) => {
        const timer = setTimeout(() => { pending.delete(id); reject(Error(method + ' timeout')); }, 4500);
        pending.set(id, message => {
          clearTimeout(timer);
          pending.delete(id);
          message.error ? reject(Error(method + ': ' + message.error.message)) : resolve(message.result);
        });
        ws.send(JSON.stringify({ id, method, params, sessionId }));
      });
    },
  };
}

try {
  let port;
  for (let i = 0; i < 80 && !port && !evidence.launchError && child.exitCode === null; i++) {
    try { port = (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split(/\r?\n/)[0]; }
    catch { await sleep(100); }
  }
  assert.ok(port, 'DevToolsActivePort missing');
  const endpoint = 'http://127.0.0.1:' + port;
  const descriptor = await (await fetch(endpoint + '/json/version', { signal: AbortSignal.timeout(4000) })).json();
  const main = await connect(descriptor.webSocketDebuggerUrl);
  const { product } = await main.send('Browser.getVersion');
  record('browser transport', 'PASS', product);
  try {
    const { processInfo } = await main.send('SystemInfo.getProcessInfo');
    const roles = {};
    for (const proc of processInfo) roles[proc.type] = (roles[proc.type] || 0) + 1;
    record('Chromium process types', 'PASS', roles);
  } catch (error) { record('Chromium process types', 'BLOCKED', error.message); }
  const targets = await (await fetch(endpoint + '/json/list', { signal: AbortSignal.timeout(4000) })).json();
  const page = targets.find(target => target.type === 'page');
  assert.ok(page, 'Tab target missing');
  const tab = await connect(page.webSocketDebuggerUrl);
  let send = tab.send;
  let events = tab.events;
  try {
    await send('Page.enable');
    record('direct Page.enable', 'PASS');
  } catch (error) {
    record('direct Page.enable', 'BLOCKED', error.message);
    const { sessionId } = await main.send('Target.attachToTarget', { targetId: page.id, flatten: true });
    record('flattened attach', 'PASS');
    send = (method, params) => main.send(method, params, sessionId);
    events = main.events;
    await send('Runtime.enable');
    record('flattened Runtime.enable', 'PASS');
    await send('Page.enable');
    record('flattened Page.enable', 'PASS');
  }
  await send('Runtime.enable');
  await send('Network.enable');
  await send('Log.enable');
  const evaluate = async expression => (await send('Runtime.evaluate', { expression, returnByValue: true })).result.value;
  async function navigate(url) {
    await send('Page.navigate', { url });
    for (let attempt = 0; attempt < 60; attempt++) {
      if (await evaluate('location.href + "|" + document.readyState') === url + '|complete') return;
      await sleep(100);
    }
    throw Error('Navigation did not complete');
  }
  await navigate(probeUrl);
  assert.equal(await evaluate('document.title'), 'BeautyShop probe');
  await evaluate('document.querySelector("#q").focus()');
  await send('Input.insertText', { text: 'serum' });
  const point = await evaluate('(() => {const r=document.querySelector("#send").getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2};})()');
  await send('Input.dispatchMouseEvent', { ...point, type: 'mousePressed', button: 'left', clickCount: 1 });
  await send('Input.dispatchMouseEvent', { ...point, type: 'mouseReleased', button: 'left', clickCount: 1 });
  assert.equal(await evaluate('document.querySelector("output").value'), 'serum');
  const image = await send('Page.captureScreenshot', { format: 'png' });
  assert.ok(Buffer.from(image.data, 'base64').length > 500);
  record('HTML, DOM, typing, click, screenshot capture', 'PASS');
  if (app) {
    await navigate(app + '/');
    assert.match(await evaluate('document.title'), /BeautyShop/);
    record('BeautyShop Home title', 'PASS');
  }
  const errors = events.filter(event => event.method === 'Runtime.exceptionThrown' || event.method === 'Network.loadingFailed'
    || (event.method === 'Log.entryAdded' && event.params.entry.level === 'error'));
  record('console and network error count', 'PASS', errors.length);
  evidence.result = app ? 'PASS_APP_SMOKE' : 'PASS_SIMPLE_HTML';
} catch (error) {
  evidence.result = 'BLOCKED';
  evidence.blocker = error.message;
  process.exitCode = 1;
} finally {
  sockets.forEach(ws => ws.close());
  child.kill();
  server.close();
  evidence.diagnostics = stderr.join('').split(/\r?\n/).filter(line => /failed to launch|renderer|persistent.cache|GPU process|sandbox|0x20/i.test(line))
    .slice(0, 24).map(line => line.replaceAll(scratch, '[isolated]'));
  await writeFile(path.join(qa, engine + '-separate-cache.json'), JSON.stringify(evidence, null, 2) + '\n');
  console.log(engine, evidence.result, evidence.blocker || '');
}
