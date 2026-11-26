import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const [base, scratch, mode] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
assert.ok(['before', 'after'].includes(mode));
const profile = path.join(scratch, 'chrome');
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless', '--remote-debugging-port=0', '--no-first-run', '--disable-extensions',
  '--disable-background-networking', '--force-prefers-reduced-motion', '--user-data-dir=' + profile, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const snapshots = [];
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
    const directory = path.resolve('docs/ui/screenshots/UI-01C');
    await mkdir(directory, { recursive: true });
    const result = await call('Page.captureScreenshot', { format: 'webp', quality: 78 });
    await writeFile(path.join(directory, name + '.webp'), Buffer.from(result.data, 'base64'));
  }
  const noOverflow = () => evaluate('document.documentElement.scrollWidth <= innerWidth');
  const focused = selector => evaluate('document.activeElement.matches(' + JSON.stringify(selector) + ')');
  const focus = selector => evaluate('document.querySelector(' + JSON.stringify(selector) + ').focus()');
  const open = selector => evaluate('document.querySelector(' + JSON.stringify(selector) + ').open');
  const app = await (await fetch(base + '/js/app.js')).text();
  await call('Page.enable');
  await call('Network.enable');
  async function blockApp(blocked) {
    await call('Network.setBlockedURLs', {
      urls: ['https://*', 'http://*'],
      urlPatterns: [
        ...(blocked ? [{ urlPattern: base + '/js/app.js', block: true }] : []),
        { urlPattern: base + '/*', block: false },
      ],
    });
  }
  const rects = () => evaluate(`(() => {
    const input = document.querySelector('.password-control input');
    const button = document.querySelector('[data-password-toggle]');
    const rect = el => {
      const { x, y, width, height } = el.getBoundingClientRect();
      return [x + scrollX, y + scrollY, width, height].map(n => Math.round(n * 100) / 100);
    };
    return { input: rect(input), button: rect(button), form: rect(input.form), font: getComputedStyle(input).fontFamily };
  })()`);
  await blockApp(true);
  for (const [width, height, dpr, layout] of [
    [320, 780, 1, '320px'], [390, 850, 1, '390px'], [768, 900, 1, '768px'],
    [1366, 950, 1, '1366px'], [683, 475, 2, '1366px-200%-equivalent'],
    [1920, 1080, 1, '1920px'],
  ]) {
    await viewport(width, height, dpr);
    for (const page of ['/login', '/register']) {
      await go(page);
      await evaluate('document.fonts.ready.then(() => true)');
      const before = await rects();
      await evaluate(app, 'Initialize app.js');
      const initialized = await rects();
      await evaluate(`(() => {
        const input = document.querySelector('.password-control input');
        input.value = 'test-selection';
        input.focus();
        input.setSelectionRange(2, 6);
      })()`);
      await key('Tab', 9);
      assert.ok(await focused('[data-password-toggle]'));
      await key(' ', 32, 'Space');
      const shown = await rects();
      assert.ok(await evaluate('document.querySelector(".password-control input").type === "text"'));
      assert.ok(await focused('[data-password-toggle]'));
      assert.ok(await evaluate('document.querySelector(".password-control input").selectionStart === 2'));
      await key(' ', 32, 'Space');
      await until(async () => assert.ok(await evaluate('document.querySelector(".password-control input").type === "password"'), page + ' hide ' + layout));
      const hidden = await rects();
      assert.ok(await noOverflow());
      if (mode === 'after') {
        assert.deepEqual(initialized, before, page + ' initialization ' + layout);
        assert.deepEqual(shown, before, page + ' show ' + layout);
        assert.deepEqual(hidden, before, page + ' hide ' + layout);
        if (width === 390 && page === '/login') {
          await evaluate('document.querySelector("#password").value = ""');
          await capture('login-390-password-focus');
        }
      }
      snapshots.push({ page, layout, before, initialized, shown, hidden });
    }
  }

  if (mode === 'after') {
    await blockApp(false);
    await viewport(390, 850);
    await go('/products?brand=fixture-brand&sort=priceAsc&size=1');
    const panel = '#catalog-filters';
    assert.ok(await open(panel));
    await focus(panel + ' summary');
    await key(' ', 32, 'Space');
    assert.equal(await open(panel), false);
    const next = await until(async () => {
      const url = await evaluate('document.querySelector("[data-catalog-filter-pages] a").href');
      assert.match(url, /filterPanel=closed/);
      return url;
    });
    assert.match(await evaluate('document.querySelector(".catalog-results .store-card").href'), /filterPanel=closed/);
    await go(next.slice(base.length));
    assert.equal(await open(panel), false);
    assert.ok(await noOverflow());
    checks.push('catalog-active-and-closed-next-page');

    const first = '/products/fixture-a?from=catalog&brand=fixture-brand&filterPanel=closed';
    await go(first);
    const instructions = '#fixture-a-huong-dan';
    const ingredients = '#fixture-a-thanh-phan';
    await focus(instructions + ' summary');
    await key('Enter', 13);
    await focus(ingredients + ' summary');
    await key(' ', 32, 'Space');
    assert.ok(await open(instructions));
    assert.ok(await open(ingredients));
    assert.equal(await open('#fixture-a-loai-da'), false);
    assert.match(await evaluate('document.querySelector(".pdp-back").href'), /filterPanel=closed/);
    await go('/products/fixture-b');
    assert.equal(await open('#fixture-b-huong-dan'), false);
    const history = await call('Page.getNavigationHistory');
    await call('Page.navigateToHistoryEntry', { entryId: history.entries[history.currentIndex - 1].id });
    await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), base + first + '|complete'));
    assert.ok(await open(instructions));
    assert.ok(await open(ingredients));
    await go('/products/fixture-a');
    assert.equal(await open(instructions), false);
    checks.push('product-independent-summaries-back-new-entry');

    await go('/products');
    await focus('.header-menu > summary');
    await key('Enter', 13);
    await focus('.nav-discovery > summary');
    await key('Enter', 13);
    await focus('.nav-group > summary');
    await key('Enter', 13);
    await focus('.header-menu > summary');
    await key('Enter', 13);
    await until(async () => assert.equal(await open('.nav-discovery'), false));
    await key('Enter', 13);
    assert.ok(await open('.header-menu'));
    assert.equal(await open('.nav-group'), false);
    await key('Escape', 27);
    assert.equal(await open('.header-menu'), false);
    assert.ok(await focused('.header-menu > summary'));
    checks.push('mobile-menu-keyboard-and-ephemeral-groups');

    // Disabled scripts test server-rendered navigation and forms.
    await call('Emulation.setScriptExecutionDisabled', { value: true });
    await go('/products?filterPanel=open#brands');
    assert.ok(await open(panel));
    await focus('#catalog-sort');
    await key('ArrowDown', 40);
    await focus('.store-filters button');
    await key('Enter', 13);
    await until(async () => assert.match(await evaluate('location.search'), /sort=priceAsc/));
    assert.ok(await open(panel));
    await go('/register');
    assert.ok(await evaluate('document.querySelector("[data-password-toggle]").hidden'));
    assert.ok(await evaluate('document.querySelector("#matKhau").type === "password"'));
    await focus('#hoTen');
    await call('Input.insertText', { text: 'Fixture Registration' });
    await focus('#email');
    await call('Input.insertText', { text: 'registration@example.invalid' });
    await focus('#matKhau');
    await call('Input.insertText', { text: 'ắ'.repeat(30) });
    await key('Enter', 13);
    await until(async () => assert.ok(await evaluate('document.querySelector("#matKhau")?.getAttribute("aria-invalid") === "true"')));
    assert.ok(await evaluate('document.querySelector("#matKhau").value === ""'));
    checks.push('no-js-catalog-sort-and-register-byte-validation');

    await go('/login');
    for (const password of ['invalid-fixture', process.env.UI01C_FIXTURE_PASS]) {
      assert.ok(password);
      await focus('#username');
      await call('Input.insertText', { text: 'admin@example.invalid' });
      await focus('#password');
      await call('Input.insertText', { text: password });
      await key('Enter', 13);
      const route = password === 'invalid-fixture' ? '/login?error' : '/admin/dashboard';
      await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), base + route + '|complete'));
    }
    const dashboard = 'details[data-chart-table]';
    await go('/admin/dashboard?range=7d');
    assert.ok(await open(dashboard));
    assert.ok(await evaluate('document.querySelector("#table-daily").querySelectorAll("tbody tr").length > 0'));
    await focus(dashboard + ' > summary');
    await key('Tab', 9);
    assert.ok(await focused('.table-scroll[aria-labelledby="chart-daily-title"]'));
    const dailyTableRegion = 'document.activeElement';
    assert.ok(await evaluate(dailyTableRegion + '.scrollWidth > ' + dailyTableRegion + '.clientWidth'));
    await key('ArrowRight', 39);
    await until(async () => assert.ok(await evaluate(dailyTableRegion + '.scrollLeft > 0')));
    assert.ok(await noOverflow());
    await capture('dashboard-390-table-nojs');
    checks.push('no-js-login-and-dashboard-table');

    const detail = '/admin/users/2?q=Fixture&page=0';
    await go(detail);
    await focus('.admin-menu > summary');
    await key('Enter', 13);
    assert.equal(await evaluate('document.querySelector(".admin-menu a[aria-current=page]").textContent.trim()'), 'Quản lý tài khoản');
    assert.ok(await evaluate('document.querySelector(".admin-menu a[aria-current=page]").getBoundingClientRect().height >= 44'));
    assert.ok(await open('.admin-menu'));
    const reset = 'details.disclosure';
    await focus(reset + ' > summary');
    await key('Enter', 13);
    assert.ok(await open(reset));
    assert.equal(await evaluate('document.querySelector("#reason").form.checkValidity()'), false);
    await focus('#reason');
    await key('ArrowDown', 40);
    assert.ok(await evaluate('document.querySelector("#reason").form.checkValidity()'));
    assert.ok(await evaluate('document.querySelector("#reason").form.querySelector("[name=_csrf]") !== null'));
    assert.ok(await noOverflow());
    checks.push('no-js-admin-mobile-current-and-reset-required');

    await call('Emulation.setScriptExecutionDisabled', { value: false });
    await viewport(1366, 950);
    await go('/admin/dashboard?range=7d');
    assert.ok(await evaluate('document.querySelectorAll("canvas[data-chart-ready=true]").length > 0'));
    assert.ok(await open(dashboard));
    await focus(dashboard + ' > summary');
    await key('Enter', 13);
    assert.equal(await open(dashboard), false);
    await go('/admin/dashboard?range=30d');
    assert.equal(await open(dashboard), false);
    assert.ok(await open('details[data-chart-table]:has(#table-status)'));
    assert.ok(await noOverflow());
    checks.push('charts-render-and-table-preference-is-independent');

    await viewport(683, 475, 2);
    await go(detail);
    await focus(reset + ' > summary');
    await key('Enter', 13);
    await focus('#reason');
    await key('ArrowDown', 40);
    await focus('button[data-confirm]');
    await key('Enter', 13);
    assert.ok(await open('[data-confirm-dialog]'));
    assert.ok(await focused('[data-confirm-cancel]'));
    await key('Escape', 27);
    assert.equal(await open('[data-confirm-dialog]'), false);
    assert.ok(await focused('button[data-confirm]'));
    await key('Enter', 13);
    assert.ok(await open('[data-confirm-dialog]'));
    await go('/admin/dashboard');
    const adminHistory = await call('Page.getNavigationHistory');
    await call('Page.navigateToHistoryEntry', { entryId: adminHistory.entries[adminHistory.currentIndex - 1].id });
    await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), base + detail + '|complete'));
    assert.equal(await open('[data-confirm-dialog]'), false);
    assert.ok(await noOverflow());
    await viewport(1366, 950);
    assert.equal(await evaluate('document.querySelector(".admin-sidebar a[aria-current=page]").textContent.trim()'), 'Quản lý tài khoản');
    checks.push('admin-reset-confirm-cancel-back-and-desktop-current');
  }
  await writeFile('docs/qa/UI-01C/browser-' + mode + '.json', JSON.stringify({ browser: Browser, snapshots, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
