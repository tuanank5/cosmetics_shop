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
    const directory = path.resolve('docs/ui/screenshots/CART-01');
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
  const hasNotice = (sessionId) => evaluate('document.querySelector("[data-cart-merge-notice]") !== null', 'Read notice', sessionId);
  async function addGuest(slug, variant, quantity) {
    await go('/products/' + slug);
    const result = await evaluate(`(async () => {
      const csrf = document.querySelector('form[action="/cart/items"] [name="_csrf"]').value;
      const requests = Array.from({ length: ${quantity} }, () => fetch('/cart/items', {
        method: 'POST', body: new URLSearchParams({ _csrf: csrf, variant: '${variant}' }),
      }));
      return (await Promise.all(requests)).every(r => r.ok);
    })()`, 'Add fixture SKU to the guest cart');
    assert.equal(result, true);
  }
  async function savedCheckout() {
    await call('Page.navigate', { url: base + '/checkout' });
    await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/login|complete'));
  }
  async function login(email) {
    await evaluate('document.querySelector("#username").focus()');
    await call('Input.insertText', { text: email });
    await evaluate('document.querySelector("#password").focus()');
    await call('Input.insertText', { text: process.env.CART01_FIXTURE_PASS });
    await key('Enter', 13);
  }
  const waitCheckout = () => until(async () => assert.equal(
    await evaluate('location.pathname + "|" + document.readyState'), '/checkout|complete'));
  async function logout() {
    await evaluate('document.querySelector(".header-account-form button").focus()');
    await key('Enter', 13);
    await until(async () => assert.equal(await evaluate('location.search + "|" + document.readyState'), '?logout|complete'));
  }

  await viewport(390, 850);
  await addGuest('fixture-a', 1, 5);
  await addGuest('fixture-b', 2, 3);
  await savedCheckout();
  let paused;
  socket.addEventListener('message', event => {
    const update = JSON.parse(event.data);
    if (update.method === 'Fetch.requestPaused') paused = update.params;
  });
  await call('Fetch.enable', { patterns: [{ urlPattern: base + '/checkout?*cartMerge=*', requestStage: 'Request' }] });
  await login('buyer@example.invalid');
  await until(() => assert.ok(paused));
  const { targetId } = await call('Target.createTarget', { url: base + '/cart' });
  const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState', 'Wait for second tab', sessionId), '/cart|complete'));
  assert.equal(await hasNotice(sessionId), false);
  assert.equal(await evaluate('document.querySelector("input[name=quantity]").value', 'Check committed cart in second tab', sessionId), '10');
  await call('Target.closeTarget', { targetId });
  await call('Fetch.continueRequest', { requestId: paused.requestId });
  await call('Fetch.disable');
  await waitCheckout();
  assert.ok(await hasNotice());
  const notice = await evaluate('document.querySelector("[data-cart-merge-notice]").textContent');
  assert.match(notice, /Dự kiến 12, thực nhận 10, giảm 2/);
  assert.match(notice, /Dự kiến 4, thực nhận 3, giảm 1/);
  assert.equal(await evaluate('document.querySelectorAll("[data-cart-merge-notice] li").length'), 2);
  assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'));
  const tagged = await evaluate('location.pathname + location.search');
  await capture('checkout-390-notice');
  checks.push('two-tabs-one-checkout-notice-for-two-stock-reductions');

  const checkoutEntry = await call('Page.getNavigationHistory');
  await go('/cart');
  await call('Page.navigateToHistoryEntry', { entryId: checkoutEntry.entries[checkoutEntry.currentIndex].id });
  await waitCheckout();
  assert.equal(await hasNotice(), false);
  await call('Page.reload');
  await waitCheckout();
  assert.equal(await hasNotice(), false);
  await logout();
  await call('Page.navigateToHistoryEntry', { entryId: checkoutEntry.entries[checkoutEntry.currentIndex].id });
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/login|complete'));
  assert.equal(await hasNotice(), false);
  checks.push('back-refresh-logout-do-not-replay-notice');

  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/login');
  await login('other@example.invalid');
  await until(async () => assert.ok(await evaluate('location.pathname !== "/login" && document.readyState === "complete"')));
  await go(tagged);
  assert.equal(await hasNotice(), false);
  await logout();
  await addGuest('fixture-a', 1, 5);
  await savedCheckout();
  await login('other@example.invalid');
  await waitCheckout();
  assert.ok(await hasNotice());
  assert.match(await evaluate('document.querySelector("[data-cart-merge-notice]").textContent'), /Dự kiến 12, thực nhận 10, giảm 2/);
  await go(await evaluate('location.pathname + location.search'));
  assert.equal(await hasNotice(), false);
  checks.push('different-owner-cannot-read-old-notice-and-no-js-works');

  await writeFile('docs/qa/CART-01/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
