import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const [base, scratch] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
const captures = path.resolve('docs/ui/screenshots/NAV-01');
await mkdir(captures, { recursive: true });
const profile = path.join(scratch, 'chrome-profile');
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless', '--remote-debugging-port=0', `--user-data-dir=${profile}`,
  '--disable-background-networking', '--disable-extensions', '--no-first-run', 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
let socket;
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
async function retry(action, timeout = 10000) {
  const deadline = Date.now() + timeout;
  for (;;) {
    try { return await action(); }
    catch (error) {
      if (Date.now() >= deadline) throw error;
      await wait(100);
    }
  }
}
try {
  const port = await retry(async () => {
    assert.equal(chrome.exitCode, null);
    return (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0];
  });
  const endpoint = `http://127.0.0.1:${port}`;
  const [tab] = (await (await fetch(endpoint + '/json/list')).json()).filter(t => t.type === 'page');
  const { Browser } = await (await fetch(endpoint + '/json/version')).json();
  socket = new WebSocket(tab.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', reject, { once: true });
  });
  let id = 0;
  const pending = new Map();
  socket.addEventListener('message', event => {
    const { id, result, error } = JSON.parse(event.data);
    pending.get(id)?.(error, result);
  });
  function call(method, params = {}, sessionId) {
    const seq = ++id;
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        pending.delete(seq);
        reject(new Error(`Chrome command timed out: ${method}`));
      }, 7000);
      pending.set(seq, (error, result) => {
        clearTimeout(timeout);
        pending.delete(seq);
        if (error) reject(new Error(error.message));
        else resolve(result);
      });
      socket.send(JSON.stringify({ id: seq, method, params, sessionId }));
    });
  }
  await call('Page.enable');
  await call('Network.enable');
  await call('Network.setBlockedURLs', {
    urls: ['https://*', 'http://*'],
    urlPatterns: [{ urlPattern: base + '/*', block: false }],
  });
  async function evaluate(expression) {
    const response = await call('Runtime.evaluate', { expression, returnByValue: true });
    assert.ok(!response.exceptionDetails, expression);
    return response.result.value;
  }
  async function navigate(url) {
    const result = await call('Page.navigate', { url });
    assert.ok(!result.errorText, result.errorText);
    await retry(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), url + '|complete'));
  }
  async function key(key, code) {
    const params = { key, code: key, windowsVirtualKeyCode: code };
    await call('Input.dispatchKeyEvent', { type: 'rawKeyDown', ...params });
    await call('Input.dispatchKeyEvent', { type: 'keyUp', ...params });
  }
  const phase = process.argv[4];
  assert.ok(['before', 'after'].includes(phase));
  await call('Emulation.setDeviceMetricsOverride', { width: 1366, height: 1000, deviceScaleFactor: 1, mobile: false });
  await call('Page.addScriptToEvaluateOnNewDocument', { source: `
    window.navTrace = [];
    addEventListener('pageshow', e => navTrace.push({ persisted: e.persisted, y: scrollY }));
  ` });
  const state = () => evaluate(`({
    path: location.pathname, hash: location.hash, y: Math.round(scrollY),
    slide: document.querySelector('[data-home-carousel]')?.dataset.activeSlide ?? null,
    fallback: document.querySelector('[data-home-carousel]')?.dataset.carouselFallback ?? null,
    focus: document.activeElement?.id ?? '', historySlide: history.state?.beautyshopHomeSlide ?? null,
    pageshow: window.navTrace, type: performance.getEntriesByType('navigation')[0]?.type
  })`);
  const click = selector => evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`);
  const historyStep = async direction => {
    const { currentIndex, entries } = await call('Page.getNavigationHistory');
    const entry = entries[currentIndex + direction];
    assert.ok(entry);
    await call('Page.navigateToHistoryEntry', { entryId: entry.id });
    await retry(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), entry.url + '|complete'));
  };
  const settle = async () => {
    await retry(async () => assert.equal(await evaluate('document.readyState'), 'complete'));
    await wait(900);
  };
  const report = {};
  await navigate(base + '/');
  await settle();
  report.opening = await state();
  await click('.home-carousel-pagination button:nth-child(4)');
  await wait(950);
  report.beforeLogo = await state();
  await click('.site-header .brand');
  await settle();
  report.logoAtHome = await state();
  await navigate(base + '/?fixture=popular#home-popular');
  await settle();
  const product = await evaluate("document.querySelector('#home-popular .store-card').href");
  const anchor = await evaluate("document.querySelector('#home-popular .store-card').id");
  report.entry = await state();
  await click('#home-popular .store-card');
  await retry(async () => assert.equal(await evaluate('location.href'), product));
  await settle();
  report.pdpBackPath = await evaluate("new URL(document.querySelector('.pdp-back').href).pathname + new URL(document.querySelector('.pdp-back').href).hash");
  if (phase === 'after') {
    await historyStep(-1);
    await settle();
    report.browserBack = await state();
    assert.equal(report.browserBack.path, '/');
    assert.equal(report.browserBack.slide, '6');
    assert.ok(Math.abs(report.browserBack.y - report.entry.y) < 100, JSON.stringify(report.browserBack));
    await historyStep(1);
    await settle();
    report.browserForward = await state();
    assert.equal(await evaluate('location.href'), product);
  }
  await click('.pdp-back');
  await settle();
  report.returnLink = await state();
  await navigate(product);
  await settle();
  await click('.site-header .brand');
  await settle();
  report.logoFromPdp = await state();
  if (phase === 'after') {
    assert.equal(report.beforeLogo.slide, '4');
    assert.equal(report.logoAtHome.slide, '1');
    assert.equal(report.logoFromPdp.slide, '1');
    assert.equal(report.pdpBackPath, '/#' + anchor);
    assert.equal(report.returnLink.focus, anchor);
    assert.equal(report.returnLink.slide, '6');
    await navigate(base + '/?fixture=missing#home-popular-product-99999999');
    await settle();
    report.missing = await state();
    assert.equal(report.missing.focus, 'popular-title');
    await navigate(product.split('?')[0]);
    report.directPdpBack = await evaluate("new URL(document.querySelector('.pdp-back').href).pathname");
    assert.equal(report.directPdpBack, '/products');
    const { targetId } = await call('Target.createTarget', { url: 'about:blank' });
    try {
      const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
      await call('Network.enable', {}, sessionId);
      await call('Network.setBlockedURLs', {
        urls: ['https://*', 'http://*'],
        urlPatterns: [{ urlPattern: base + '/*', block: false }],
      }, sessionId);
      await call('Page.navigate', { url: product }, sessionId);
      await retry(async () => {
        const { result } = await call('Runtime.evaluate', {
          expression: "document.readyState === 'complete' && document.querySelector('.pdp-back')?.href",
          returnByValue: true,
        }, sessionId);
        assert.equal(result.value, base + '/#' + anchor);
      });
      report.freshTab = true;
    } finally {
      await call('Target.closeTarget', { targetId });
    }
    await call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
    await navigate(base + '/?fixture=reduced#' + anchor);
    await settle();
    report.reduced = await state();
    assert.equal(report.reduced.fallback, 'reduced-motion');
    assert.equal(report.reduced.focus, anchor);
    await call('Emulation.setDeviceMetricsOverride', { width: 390, height: 850, deviceScaleFactor: 1, mobile: false });
    await call('Emulation.setScriptExecutionDisabled', { value: true });
    await navigate(product);
    let keyboardBack = false;
    for (let i = 0; i < 40; i++) {
      await key('Tab', 9);
      if (await evaluate("document.activeElement.matches('.pdp-back:focus-visible')")) {
        keyboardBack = true;
        break;
      }
    }
    assert.ok(keyboardBack, 'Back link must be keyboard accessible without site scripts');
    await key('Enter', 13);
    await settle();
    report.noJs = await state();
    report.keyboardBack = keyboardBack;
    assert.equal(report.noJs.hash, '#' + anchor);
    assert.ok(await evaluate(`(() => {
      const el = document.getElementById(${JSON.stringify(anchor)});
      const rect = el?.getBoundingClientRect();
      return rect && rect.bottom > 0 && rect.top < innerHeight &&
         document.documentElement.clientWidth === document.documentElement.scrollWidth;
    })()`));
    const { data } = await call('Page.captureScreenshot', { format: 'png' });
    await writeFile(path.join(captures, 'popular-return-390-nojs.png'), Buffer.from(data, 'base64'));
  }
  for (const entry of Object.values(report)) {
    if (entry?.path?.startsWith('/products/')) entry.path = '/products/{slug}';
  }
  await writeFile(`docs/qa/NAV-01/browser-${phase}.json`, JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report));
  await call('Browser.close').catch(() => {});
} finally {
  socket?.close();
  if (chrome.exitCode === null) chrome.kill();
}
