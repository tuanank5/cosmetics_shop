import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const [base, scratch] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
const captures = path.resolve('docs/ui/screenshots/DATA-01');
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
  function call(method, params = {}) {
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
      socket.send(JSON.stringify({ id: seq, method, params }));
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
  const results = [];
  for (const width of [390, 768, 1366]) {
    await call('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: false });
    await call('Emulation.setScriptExecutionDisabled', { value: true });
    await navigate(base + '/products');
    const card = await evaluate(`(() => {
      const card = [...document.querySelectorAll('.store-card')].find(c => c.textContent.includes('Sản phẩm Mongo tắt'));
      const label = [...card.querySelectorAll('p')].find(p => p.textContent.includes('Đánh giá tạm thời không khả dụng'));
      const link = card.matches('a') ? card : card.querySelector('a[href*="/products/"]');
      const rect = label.getBoundingClientRect();
      const { clientWidth, scrollWidth } = document.documentElement;
      return { href: link.href, width: innerWidth, clientWidth, scrollWidth,
        visible: rect.width > 0 && rect.height > 0 && rect.left >= 0 && rect.right <= clientWidth,
        noStars: !card.querySelector('.store-card-rating') };
    })()`);
    assert.equal(card.width, width);
    assert.equal(card.scrollWidth, card.clientWidth);
    assert.ok(card.visible && card.noStars, JSON.stringify(card));
    assert.ok(card.href.startsWith(base + '/products/'));
    const focus = [];
    for (let i = 0; i < 120; i++) {
      await key('Tab', 9);
      const current = await evaluate(`(() => {
        const a = document.activeElement;
        const css = getComputedStyle(a);
        return { href: a.href, focusVisible: a.matches(':focus-visible'),
          outline: css.outlineStyle !== 'none' && parseFloat(css.outlineWidth) > 0 };
      })()`);
      if (current.href === card.href) { focus.push(current); break; }
    }
    assert.ok(focus[0]?.focusVisible && focus[0]?.outline, 'Product link must show keyboard focus');
    if (width === 390) {
      const { data } = await call('Page.captureScreenshot', { format: 'png' });
      await writeFile(path.join(captures, 'catalog-390-keyboard.png'), Buffer.from(data, 'base64'));
    }
    await key('Enter', 13);
    const detail = await retry(async () => {
      assert.equal(await evaluate('location.href + "|" + document.readyState'), card.href + '|complete');
      return await evaluate(`(() => {
        const label = [...document.querySelectorAll('main p')].find(p => p.textContent.includes('Tạm thời chưa tải được đánh giá'));
        const rect = label.getBoundingClientRect();
        label.scrollIntoView({block:'center'});
        const { clientWidth, scrollWidth } = document.documentElement;
        return { visible: rect.width > 0 && rect.height > 0 && rect.left >= 0 && rect.right <= clientWidth,
          clientWidth, scrollWidth };
      })()`);
    });
    assert.equal(detail.scrollWidth, detail.clientWidth);
    assert.ok(detail.visible);
    if (width === 1366) {
      const { data } = await call('Page.captureScreenshot', { format: 'png' });
      await writeFile(path.join(captures, 'product-1366-nojs.png'), Buffer.from(data, 'base64'));
    }
    results.push({ width, siteJavaScriptDisabled: true, noHorizontalOverflow: true,
      catalogUnavailableVisible: true, keyboardFocusVisible: true, enterOpensProduct: true, productUnavailableVisible: true });
  }
  await writeFile('docs/qa/DATA-01/browser-results.json', JSON.stringify({ browser: Browser, mongo: 'off', results }, null, 2) + '\n');
  console.log(`${Browser}: ${results.length} viewports passed`);
  await call('Browser.close').catch(() => {});
} finally {
  socket?.close();
  if (chrome.exitCode === null) chrome.kill();
}
