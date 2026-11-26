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
  function call(method, params = {}) {
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
      socket.send(JSON.stringify({ id, method, params }));
    });
  }
  async function evaluate(expression, label = 'Browser evaluation') {
    const result = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
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
    const directory = path.resolve('docs/ui/screenshots/DTO-01');
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
  await viewport(390, 850);
  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/login');
  await evaluate('document.querySelector("#username").focus()');
  await call('Input.insertText', { text: 'buyer@example.invalid' });
  await evaluate('document.querySelector("#password").focus()');
  await call('Input.insertText', { text: process.env.DTO01_FIXTURE_PASS });
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/|complete'));

  await go('/checkout');
  assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'));
  assert.equal(await evaluate('document.querySelector("#addressId option:checked").textContent.trim()'), 'Fixture Buyer · Fixture address');
  await evaluate('document.querySelector("#addressId").focus()');
  await key('Tab', 9);
  assert.ok(await evaluate('document.activeElement.matches(\'a[href="/account/addresses?from=checkout"]:focus-visible\')'));
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + location.search + "|" + document.readyState'), '/account/addresses?from=checkout|complete'));
  assert.ok(await evaluate('document.querySelector("main").textContent.includes("Fixture address")'));
  assert.ok(await evaluate('!document.querySelector("main").textContent.includes("Other fixture address")'));
  await capture('mobile-owned-address');
  await evaluate('document.querySelector(\'main a[href="/checkout"]\').focus()');
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/checkout|complete'));
  checks.push('mobile-no-js-keyboard-address-return-and-ownership');

  await viewport(1280, 900);
  await evaluate('document.querySelector(\'button[formaction="/checkout/quote"]\').focus()');
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/checkout/quote|complete'));
  assert.ok(await evaluate('document.querySelector("[data-checkout-form]").dataset.quoteValid === "true"'));
  assert.ok(await evaluate('document.querySelector("main").textContent.includes("Địa chỉ báo giá: Fixture Buyer · Fixture address")'));
  assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'));
  await capture('desktop-quote');
  checks.push('desktop-no-js-quote-renders-selected-address-and-server-total');

  await writeFile('docs/qa/DTO-01/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
