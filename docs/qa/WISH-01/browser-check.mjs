import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const [base, scratch] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
const profile = path.join(scratch, 'chrome');
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless', '--remote-debugging-port=0', '--no-first-run', '--disable-extensions',
  '--disable-background-networking', '--force-prefers-reduced-motion', '--user-data-dir=' + profile, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const checks = [];
let socket;

async function until(check) {
  const end = Date.now() + 12000;
  for (;;) {
    try { return await check(); }
    catch (error) {
      if (Date.now() >= end) throw error;
      await new Promise(resolve => setTimeout(resolve, 120));
    }
  }
}
try {
  const port = await until(async () => (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]);
  const endpoint = 'http://127.0.0.1:' + port;
  const { Browser } = await (await fetch(endpoint + '/json/version')).json();
  const [tab] = (await (await fetch(endpoint + '/json/list')).json()).filter(target => target.type === 'page');
  socket = new WebSocket(tab.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', reject, { once: true });
  });
  let sequence = 0;
  const requests = new Map();
  socket.addEventListener('message', event => {
    const { id, result, error } = JSON.parse(event.data);
    requests.get(id)?.(error, result);
  });
  function call(method, params = {}, sessionId) {
    const id = ++sequence;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        requests.delete(id);
        reject(new Error(method + ' timed out'));
      }, 12000);
      requests.set(id, (error, result) => {
        clearTimeout(timer);
        requests.delete(id);
        if (error) reject(new Error(method + ': ' + error.message));
        else resolve(result);
      });
      socket.send(JSON.stringify({ id, method, params, sessionId }));
    });
  }
  async function evaluate(expression, label = 'Browser evaluation', sessionId) {
    const result = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }, sessionId);
    assert.ok(!result.exceptionDetails, label);
    return result.result.value;
  }
  async function go(route) {
    const url = base + route;
    const response = await call('Page.navigate', { url });
    assert.ok(!response.errorText, response.errorText);
    await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), url + '|complete'));
  }
  async function key(key, windowsVirtualKeyCode, code = key) {
    const params = { key, code, windowsVirtualKeyCode };
    const text = key === 'Enter' ? '\r' : undefined;
    await call('Input.dispatchKeyEvent', { type: 'keyDown', ...params, text });
    await call('Input.dispatchKeyEvent', { type: 'keyUp', ...params });
  }
  function viewport(width, height, dpr = 1) {
    return call('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: dpr, mobile: false });
  }
  async function capture(name) {
    const directory = path.resolve('docs/ui/screenshots/WISH-01');
    await mkdir(directory, { recursive: true });
    const result = await call('Page.captureScreenshot', { format: 'webp', quality: 78 });
    await writeFile(path.join(directory, name + '.webp'), Buffer.from(result.data, 'base64'));
  }

  await call('Page.enable');
  await call('Network.enable');
  await call('Network.setBlockedURLs', {
    urls: ['https://*', 'http://*'],
    urlPatterns: [{ urlPattern: base + '/*', block: false }],
  });
  const products = '/products?q=Fixture&brand=fixture-brand&sort=priceAsc&size=1&filterPanel=closed';
  const pdp = '/products/fixture-serum?variant=2';
  const forms = 'form[data-wishlist-id="1"]';
  const feedback = () => evaluate('document.querySelector("[data-wishlist-feedback]").textContent');
  const buttons = sessionId => evaluate(`[...document.querySelectorAll(${JSON.stringify(forms + ' button')})].map(b => b.getAttribute('aria-pressed'))`, 'Read heart states', sessionId);
  const waitButtons = (count, pressed, sessionId) => until(async () =>
    assert.deepEqual(await buttons(sessionId), Array(count).fill(String(pressed))));
  const submit = (sessionId) => evaluate(`document.querySelector(${JSON.stringify(forms)}).requestSubmit()`, 'Submit wishlist form', sessionId);
  const waitLocation = (pathname, hash = '') => until(async () =>
    assert.equal(await evaluate('location.pathname + "|" + location.hash + "|" + document.readyState'), pathname + '|' + hash + '|complete'));
  const focus = selector => evaluate('document.querySelector(' + JSON.stringify(selector) + ').focus()');

  await viewport(390, 850);
  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go(pdp);
  assert.equal(await evaluate(`(async () => {
    const csrf = document.querySelector('form[action="/cart/items"] [name="_csrf"]').value;
    const results = await Promise.all(Array.from({length: 5}, () => fetch('/cart/items', {
      method: 'POST', body: new URLSearchParams({ _csrf: csrf, variant: '1' }),
    })));
    return results.every(response => response.ok);
  })()`), true);
  await go(products);
  await focus('form[data-wishlist-origin="catalog"] button');
  await key('Enter', 13);
  await waitLocation('/login');
  await focus('#username');
  await call('Input.insertText', { text: 'buyer@example.invalid' });
  await focus('#password');
  await call('Input.insertText', { text: process.env.WISH01_FIXTURE_PASS });
  await key('Enter', 13);
  await waitLocation('/products', '#catalog-product-1');
  assert.equal(await evaluate('new URLSearchParams(location.search).get("filterPanel")'), 'closed');
  assert.equal(await evaluate('new URLSearchParams(location.search).get("brand")'), 'fixture-brand');
  assert.equal(await evaluate('new URLSearchParams(location.search).get("size")'), '1');
  assert.ok(await evaluate('document.querySelector("[data-cart-merge-notice]").textContent.includes("Dự kiến 12, thực nhận 10, giảm 2")'));
  await waitButtons(1, false);
  await focus(forms + ' button');
  await key('Enter', 13);
  await waitButtons(1, true);
  await waitLocation('/products', '#catalog-product-1');
  assert.equal(await evaluate('new URLSearchParams(location.search).has("cartMerge")'), false);
  await focus(forms + ' button');
  await key('Enter', 13);
  await waitButtons(1, false);
  checks.push('guest-login-origin-cart-notice-and-no-js-prg-both-directions');

  await call('Emulation.setScriptExecutionDisabled', { value: false });
  await go('/');
  await waitButtons(3, false);
  const home = await call('Page.getNavigationHistory');
  let requestsToSave = 0;
  socket.addEventListener('message', event => {
    const update = JSON.parse(event.data);
    if (update.method === 'Network.requestWillBeSent' && update.params.request.method === 'POST'
        && update.params.request.url === base + '/account/wishlist/1') requestsToSave++;
  });
  await evaluate(`(() => {
    const form = document.querySelector(${JSON.stringify(forms)});
    form.requestSubmit();
    form.requestSubmit();
  })()`);
  await waitButtons(3, true);
  assert.equal(requestsToSave, 1);
  assert.equal(await evaluate('location.pathname'), '/');
  checks.push('home-three-cards-stay-in-sync-after-rapid-clicks');

  const { targetId } = await call('Target.createTarget', { url: base + pdp });
  const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
  await waitButtons(1, true, sessionId);
  await submit(sessionId);
  await waitButtons(1, false, sessionId);
  await call('Page.bringToFront');
  await waitButtons(3, false);
  await go(products);
  await submit(sessionId);
  await waitButtons(1, true, sessionId);
  await call('Page.navigateToHistoryEntry', { entryId: home.entries[home.currentIndex].id });
  await waitLocation('/');
  await waitButtons(3, true);
  await call('Target.closeTarget', { targetId });
  checks.push('pdp-and-home-sync-across-two-tabs-and-back');

  await go(pdp);
  await waitButtons(1, true);
  assert.equal(await evaluate('new URLSearchParams(location.search).get("variant")'), '2');
  await evaluate('document.querySelector("[data-wishlist-id] button").scrollIntoView({block:"center"})');
  await capture('pdp-390-saved');
  const failedRequests = [];
  socket.addEventListener('message', event => {
    const update = JSON.parse(event.data);
    if (update.method === 'Fetch.requestPaused' && update.params.request.method === 'POST') {
      failedRequests.push(call('Fetch.failRequest', { requestId: update.params.requestId, errorReason: 'Failed' }));
    }
  });
  await call('Fetch.enable', { patterns: [{ urlPattern: base + '/account/wishlist/1/delete', requestStage: 'Response' }] });
  await submit();
  await until(() => assert.equal(failedRequests.length, 1));
  await Promise.all(failedRequests);
  await waitButtons(1, false);
  assert.match(await feedback(), /Đã đọc lại trạng thái/);
  assert.equal(failedRequests.length, 1);
  await call('Fetch.disable');
  checks.push('response-loss-after-write-reconciles-with-one-get-and-no-post-replay');

  await submit();
  await waitButtons(1, true);
  await go('/account/wishlist');
  await focus(forms + ' button');
  await key('Enter', 13);
  await until(async () => assert.ok(await evaluate('document.querySelector("[data-wishlist-empty]").hidden === false')));
  assert.ok(await evaluate('document.activeElement.hasAttribute("data-wishlist-heading")'));
  assert.equal(await evaluate('location.pathname'), '/account/wishlist');
  checks.push('last-wishlist-removal-keeps-page-and-focuses-heading');
  await writeFile('docs/qa/WISH-01/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
