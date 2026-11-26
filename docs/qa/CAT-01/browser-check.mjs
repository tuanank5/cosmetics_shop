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
    const directory = path.resolve('docs/ui/screenshots/CAT-01');
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
  await call('Input.insertText', { text: 'admin@example.invalid' });
  await evaluate('document.querySelector("#password").focus()');
  await call('Input.insertText', { text: process.env.CAT01_FIXTURE_PASS });
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/admin/dashboard|complete'));

  const pathFor = (section, id) => '/admin/catalog/' + section + '/' + id + '/status';
  const input = route => 'form[action="' + route + '"] input[name="active"]';
  const nextAction = route => evaluate('document.querySelector(' + JSON.stringify(input(route)) + ')?.value');
  async function submitFromKeyboard(route) {
    const form = 'form[action="' + route + '"]';
    await evaluate('document.querySelector(' + JSON.stringify('li:has(' + form + ') .catalog-actions a:last-of-type') + ').focus()');
    await key('Tab', 9);
    assert.ok(await evaluate('document.activeElement.matches(' + JSON.stringify(form + ' button:focus-visible') + ')'));
    assert.ok(await evaluate('document.activeElement.getBoundingClientRect().height >= 44'));
    await key('Enter', 13);
  }
  const publicStatus = route => evaluate('fetch(' + JSON.stringify(route) + ').then(res => res.status)');
  const noOverflow = () => evaluate('document.documentElement.scrollWidth <= innerWidth');
  const serum = '/products/fixture-serum';
  const hidden = '/products/fixture-hidden';

  await go('/admin/catalog/categories');
  const category = pathFor('categories', 1);
  assert.equal(await nextAction(category), 'false');
  assert.equal(await publicStatus(serum), 200);
  await submitFromKeyboard(category);
  await until(async () => assert.equal(await nextAction(category), 'true'));
  assert.equal(await publicStatus(serum), 404);
  assert.ok(await noOverflow());
  await capture('categories-390-reactivate-nojs');
  await submitFromKeyboard(category);
  await until(async () => assert.equal(await nextAction(category), 'false'));
  await go(serum);
  assert.ok(await evaluate('document.querySelector(".pdp-options").textContent.includes("FIXTURE-ACTIVE")'));
  assert.ok(await evaluate('!document.querySelector(".pdp-options").textContent.includes("FIXTURE-INACTIVE")'));
  assert.equal(await publicStatus(hidden), 404);
  checks.push('category-keyboard-nojs-stop-and-reactivate-preserves-disabled-children');

  await go('/admin/catalog/brands');
  const brand = pathFor('brands', 1);
  await submitFromKeyboard(brand);
  await until(async () => assert.equal(await nextAction(brand), 'true'));
  assert.equal(await publicStatus(serum), 404);
  await submitFromKeyboard(brand);
  await until(async () => assert.equal(await nextAction(brand), 'false'));
  assert.equal(await publicStatus(serum), 200);
  assert.ok(await noOverflow());
  checks.push('brand-keyboard-nojs-stop-and-reactivate');

  for (const section of ['categories', 'brands']) {
    await go('/admin/catalog/' + section);
    const route = pathFor(section, 2);
    const old = route.replace('/status', '/delete');
    const status = await evaluate(`(() => {
      const form = document.querySelector(${JSON.stringify('form[action="' + route + '"]')});
      const csrf = form.querySelector('[name="_csrf"]').value;
      return fetch(${JSON.stringify(old)}, { method: 'POST', body: new URLSearchParams({ _csrf: csrf }) }).then(res => res.status);
    })()`, 'Try retired CAT-01 route with fixture CSRF');
    assert.equal(status, 404);
    await go('/admin/catalog/' + section);
    assert.equal(await nextAction(route), 'false');
  }
  checks.push('retired-routes-return-404-for-unused-category-and-brand');

  await writeFile('docs/qa/CAT-01/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
