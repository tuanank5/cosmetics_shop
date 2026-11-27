import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import path from 'node:path';

const [browser, bin, target, mode] = process.argv.slice(2);
const host = target === '-' ? null : target;
assert.ok(/^(chrome|edge)$/.test(browser));
const qa = path.resolve('docs/qa/BIZ-UX-02');
const profile = path.resolve('target/biz-ux-02', browser + '-' + Date.now());
await mkdir(profile, { recursive: true });
await mkdir(path.join(qa, 'screenshots', 'before'), { recursive: true });
await mkdir(path.join(qa, 'screenshots', 'after'), { recursive: true });

const server = createServer((req, res) => {
  if (req.url === '/favicon.ico') return res.writeHead(204).end();
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.end('<!doctype html><html lang="vi"><title>Browser probe</title><label for="q">Tìm</label><input id="q"><button id="go" type="button">Đọc</button><output></output><script>go.onclick=()=>document.querySelector("output").value=q.value;</script></html>');
});
await new Promise((resolve, reject) => server.once('error', reject).listen(0, '127.0.0.1', resolve));
const local = 'http://127.0.0.1:' + server.address().port;
const evidence = { browser, version: null, steps: [], renderer: 'UNKNOWN' };
const browserProcess = spawn(bin, [
  '--headless', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--disable-extensions',
  ...(mode === 'cpu' ? ['--disable-gpu-compositing', '--disable-software-rasterizer', '--disable-features=SkiaGraphite'] : []),
  '--disable-background-networking', '--enable-logging=stderr', '--remote-debugging-port=0',
  '--user-data-dir=' + profile, local,
], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
const stderr = [];
browserProcess.stderr.on('data', data => stderr.push(String(data)));
browserProcess.on('error', error => { evidence.launchError = error.code; });
const sockets = [];
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));

async function openSocket(url) {
  const socket = new WebSocket(url);
  sockets.push(socket);
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(Error('WebSocket open timeout')), 5000);
    socket.addEventListener('open', () => { clearTimeout(timer); resolve(); }, { once: true });
    socket.addEventListener('error', () => { clearTimeout(timer); reject(Error('WebSocket open failed')); }, { once: true });
  });
  let seq = 0;
  const pending = new Map();
  const events = [];
  socket.addEventListener('message', ({ data }) => {
    const event = JSON.parse(data);
    if (event.id) pending.get(event.id)?.(event);
    else events.push(event);
  });
  return {
    events,
    send(method, params = {}) {
      return new Promise((resolve, reject) => {
        const id = ++seq;
        const timer = setTimeout(() => { pending.delete(id); reject(Error(method + ' timeout')); }, 6500);
        pending.set(id, message => {
          clearTimeout(timer);
          pending.delete(id);
          if (message.error) reject(Error(method + ': ' + message.error.message));
          else resolve(message.result);
        });
        socket.send(JSON.stringify({ id, method, params }));
      });
    },
  };
}

try {
  let port;
  for (let i = 0; i < 100 && !port && !evidence.launchError && browserProcess.exitCode === null; i++) {
    try { port = (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split(/\r?\n/)[0]; }
    catch { await wait(100); }
  }
  assert.ok(port, 'DevToolsActivePort missing');
  const endpoint = 'http://127.0.0.1:' + port;
  const version = await (await fetch(endpoint + '/json/version')).json();
  evidence.version = version.Browser;
  const main = await openSocket(version.webSocketDebuggerUrl);
  const { product } = await main.send('Browser.getVersion');
  evidence.steps.push({ name: 'browser-target CDP', result: 'PASS', product });
  const targets = await main.send('Target.getTargets');
  evidence.targets = targets.targetInfos.map(({ type, attached }) => ({ type, attached }));
  const pages = await (await fetch(endpoint + '/json/list')).json();
  const page = pages.find(item => item.type === 'page');
  assert.ok(page, 'Page target missing');
  const tab = await openSocket(page.webSocketDebuggerUrl);
  evidence.steps.push({ name: 'page-target WebSocket', result: 'PASS' });
  await tab.send('Page.enable');
  evidence.steps.push({ name: 'Page.enable', result: 'PASS' });
  await tab.send('Runtime.enable');
  await tab.send('Network.enable');
  await tab.send('Log.enable');
  const evalInPage = async expression => (await tab.send('Runtime.evaluate', { expression, returnByValue: true })).result.value;
  const navigate = async url => {
    await tab.send('Page.navigate', { url });
    for (let i = 0; i < 80; i++) {
      if (await evalInPage('location.href + "|" + document.readyState') === url + '|complete') return;
      await wait(100);
    }
    throw Error('Navigation incomplete');
  };
  await navigate(local + '/');
  assert.equal(await evalInPage('document.title'), 'Browser probe');
  await evalInPage('document.querySelector("#q").focus()');
  await tab.send('Input.insertText', { text: 'BeautyShop' });
  const point = await evalInPage('(() => { const r=go.getBoundingClientRect(); return {x:r.x+r.width/2,y:r.y+r.height/2}; })()');
  await tab.send('Input.dispatchMouseEvent', { ...point, type: 'mousePressed', button: 'left', clickCount: 1 });
  await tab.send('Input.dispatchMouseEvent', { ...point, type: 'mouseReleased', button: 'left', clickCount: 1 });
  assert.equal(await evalInPage('document.querySelector("output").value'), 'BeautyShop');
  const shot = await tab.send('Page.captureScreenshot', { format: 'png' });
  assert.ok(Buffer.from(shot.data, 'base64').length > 500);
  evidence.steps.push({ name: 'local HTML DOM, pointer, typing, screenshot', result: 'PASS' });
  if (host) {
    assert.equal(new URL(host).hostname, '127.0.0.1');
    await navigate(host + '/');
    assert.match(await evalInPage('document.title'), /BeautyShop/);
    evidence.steps.push({ name: 'BeautyShop navigation and DOM', result: 'PASS' });
    if (mode === 'before' || mode === 'after') {
      evidence.viewports = [];
      for (const width of [320, 360, 390, 768, 1024, 1280, 1440, 1920]) {
        await tab.send('Emulation.setDeviceMetricsOverride', { width, height: 844, deviceScaleFactor: 1, mobile: false });
        await navigate(host + '/#home-advisor');
        await evalInPage('document.getElementById("home-advisor").scrollIntoView({block:"center"})');
        await wait(300);
        const layout = await evalInPage('({width:innerWidth,scroll:document.documentElement.scrollWidth,carousel:document.querySelector("[data-home-carousel]").classList.contains("is-ready")})');
        evidence.viewports.push(layout);
        assert.equal(layout.width, width);
        assert.ok(layout.scroll <= width + 1, 'Horizontal overflow at ' + width);
        if (width === 390 || width === 1440) {
          const section = await tab.send('Page.captureScreenshot', { format: 'webp', quality: 78 });
          await writeFile(path.join(qa, 'screenshots', mode, 'home-advisor-' + width + '.webp'), Buffer.from(section.data, 'base64'));
        }
      }
    }
    evidence.renderer = 'PASS';
  } else evidence.renderer = 'PASS_PROBE_ONLY';
  evidence.pageErrors = tab.events.filter(e => e.method === 'Runtime.exceptionThrown' ||
    e.method === 'Network.loadingFailed' || e.method === 'Log.entryAdded' && e.params.entry.level === 'error')
    .map(e => ({ method: e.method, error: e.params.errorText || e.params.exceptionDetails?.text || e.params.entry?.source }));
} catch (err) {
  evidence.error = err.message;
  evidence.renderer = 'BLOCKED';
  process.exitCode = 1;
} finally {
  sockets.forEach(socket => socket.close());
  browserProcess.kill();
  server.close();
  const diagnostics = stderr.join('').split(/\r?\n/).filter(line => /renderer|sandbox|zygote|failed to launch|denied|crash/i.test(line))
    .slice(0, 15).map(line => line.replaceAll(profile, '[profile]'));
  const name = [browser, mode, 'probe.json'].filter(Boolean).join('-');
  await writeFile(path.join(qa, name), JSON.stringify({ ...evidence, diagnostics }, null, 2) + '\n');
  console.log(evidence.renderer, browser, evidence.error || '');
}
