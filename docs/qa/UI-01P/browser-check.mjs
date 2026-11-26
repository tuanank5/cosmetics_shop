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
    const directory = path.resolve('docs/ui/screenshots/UI-01P');
    await mkdir(directory, { recursive: true });
    const result = await call('Page.captureScreenshot', { format: 'webp', quality: 78 });
    await writeFile(path.join(directory, name + '.webp'), Buffer.from(result.data, 'base64'));
  }
  const pager = 'nav.pagination';
  const visibleNumbers = () => evaluate('[...document.querySelectorAll("nav.pagination .pagination-number, nav.pagination .pagination-current")].filter(x => x.getClientRects().length).map(x => Number(x.textContent.replace("Trang", "").trim()))');
  const noOverflow = () => evaluate('document.documentElement.scrollWidth <= innerWidth');
  const focus = selector => evaluate('document.querySelector(' + JSON.stringify(selector) + ').focus()');
  const query = () => evaluate('Object.fromEntries(new URLSearchParams(location.search))');
  const ends = () => evaluate('[...document.querySelectorAll("nav.pagination a:not(.pagination-number)")].map(link => link.textContent.trim())');
  await call('Page.enable');
  await call('Network.enable');
  await call('Network.setBlockedURLs', {
    urls: ['https://*', 'http://*'],
    urlPatterns: [{ urlPattern: base + '/*', block: false }],
  });
  const catalog = '/products?q=Fixture&brand=fixture-brand&category=fixture-group&skinType=Fixture%20skin&sort=priceAsc&size=1&filterPanel=closed';
  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await viewport(390, 850);
  await go(catalog + '&page=4');
  assert.deepEqual(await visibleNumbers(), [4, 5, 6]);
  assert.deepEqual(await ends(), ['Trang đầu', 'Trang trước', 'Trang sau', 'Trang cuối']);
  assert.ok(await evaluate('document.querySelectorAll("nav.pagination span[aria-current=page]").length === 1'));
  assert.ok(await noOverflow());
  await focus(pager + ' a');
  await key('Tab', 9);
  assert.ok(await evaluate('document.activeElement.textContent.trim() === "Trang trước" && document.activeElement.matches(":focus-visible")'));
  await key('Enter', 13);
  await until(async () => assert.deepEqual(await query(), {
    q: 'Fixture', brand: 'fixture-brand', category: 'fixture-group', skinType: 'Fixture skin',
    sort: 'priceAsc', size: '1', filterPanel: 'closed', page: '3',
  }));
  const history = await call('Page.getNavigationHistory');
  await call('Page.navigateToHistoryEntry', { entryId: history.entries[history.currentIndex - 1].id });
  await until(async () => assert.deepEqual(await visibleNumbers(), [4, 5, 6]));
  await evaluate('document.querySelector("nav.pagination").scrollIntoView({ block: "center" })');
  await capture('catalog-390-nojs');
  checks.push('catalog-mobile-no-js-keyboard-and-back');

  await viewport(1366, 950);
  await go(catalog + '&page=4');
  assert.deepEqual(await visibleNumbers(), [3, 4, 5, 6, 7]);
  await go(catalog + '&page=3');
  assert.equal(await evaluate('[...document.querySelectorAll("nav.pagination [aria-hidden=true]")].filter(x => x.getClientRects().length).length'), 1);
  await go(catalog + '&page=0');
  assert.deepEqual(await ends(), ['Trang sau', 'Trang cuối']);
  await go(catalog + '&page=99');
  assert.deepEqual(await ends(), ['Trang đầu', 'Trang trước']);
  assert.equal(await evaluate('document.querySelector(".pagination-status").textContent.trim()'), 'Trang 9 / 9');
  await go('/products?q=nonexistent');
  assert.ok(await evaluate('document.querySelector("nav.pagination") === null'));
  await go('/products?q=FIXTURE-A-SELECTED');
  assert.ok(await evaluate('document.querySelector("nav.pagination") === null'));
  checks.push('catalog-desktop-window-first-last-clamp-empty-single');

  await viewport(390, 850);
  await go('/products/fixture-a?variant=10&reviewPage=3&from=catalog&brand=fixture-brand#danh-gia');
  assert.ok(await evaluate('document.querySelector("button[aria-current=true][name=variant]").value === "10"'));
  await focus(pager + ' a');
  await key('Enter', 13);
  await until(async () => {
    const params = await query();
    assert.equal(params.reviewPage, '0');
    assert.equal(params.variant, '10');
    assert.equal(params.brand, 'fixture-brand');
    assert.equal(await evaluate('location.hash'), '#danh-gia');
    assert.equal(await evaluate('document.readyState'), 'complete');
  });
  assert.deepEqual(await ends(), ['Trang sau', 'Trang cuối']);
  assert.ok(await noOverflow());
  await go('/products/fixture-a?variant=10&reviewPage=99#danh-gia');
  assert.equal(await evaluate('document.querySelector(".pagination-status").textContent.trim()'), 'Trang 5 / 5');
  checks.push('public-reviews-preserve-selected-variant-source-anchor');

  await go('/login');
  await focus('#username');
  await call('Input.insertText', { text: 'admin@example.invalid' });
  await focus('#password');
  await call('Input.insertText', { text: process.env.UI01P_FIXTURE_PASS });
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/admin/dashboard|complete'));
  await go('/admin/users?q=Pager%20User&page=1');
  assert.deepEqual(await ends(), ['Trang đầu', 'Trang trước']);
  assert.equal(await evaluate('new URL(document.querySelector("nav.pagination a").href).searchParams.get("q")'), 'Pager User');
  await go('/admin/users?q=Pager%20User%2016&page=99');
  assert.ok(await evaluate('document.querySelector("nav.pagination") === null'));
  await go('/admin/reviews?status=DANG_HIEN&stars=4&product=1&page=1');
  assert.deepEqual(await ends(), ['Trang đầu', 'Trang trước', 'Trang sau', 'Trang cuối']);
  const reviewLink = await evaluate('[...document.querySelectorAll("nav.pagination a")].at(-1).href');
  await go(reviewLink.slice(base.length));
  assert.deepEqual(await query(), { status: 'DANG_HIEN', stars: '4', product: '1', page: '2' });
  assert.deepEqual(await ends(), ['Trang đầu', 'Trang trước']);
  await go('/admin/reviews?status=DANG_HIEN&stars=4&product=1&page=99');
  assert.equal(await evaluate('document.querySelector(".pagination-status").textContent.trim()'), 'Trang 3 / 3');
  assert.ok(await noOverflow());
  checks.push('admin-users-single-and-review-filtered-last');

  await call('Emulation.setScriptExecutionDisabled', { value: false });
  await go('/products?brand=fixture-brand&size=1');
  const filter = '#catalog-filters';
  assert.ok(await evaluate('document.querySelector("#catalog-filters").open'));
  await focus(filter + ' summary');
  await key(' ', 32, 'Space');
  await until(async () => assert.ok(await evaluate('[...document.querySelectorAll("nav.pagination a")].every(link => new URL(link.href).searchParams.get("filterPanel") === "closed")')));
  checks.push('catalog-g5-filter-panel-updates-all-pager-links');

  await writeFile('docs/qa/UI-01P/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
