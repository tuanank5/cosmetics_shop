import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const [base, scratch, phase] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));
async function until(check, timeout = 12000) {
  const deadline = Date.now() + timeout;
  for (;;) {
    try { return await check(); }
    catch (error) {
      if (Date.now() >= deadline) throw error;
      await delay(100);
    }
  }
}
const profile = path.join(scratch, 'chrome');
const chrome = spawn(process.env.BIZ_UX_CHROME, [
  '--headless', '--disable-gpu', '--no-zygote', '--single-process',
  '--remote-debugging-port=0', '--no-first-run', '--disable-extensions',
  '--disable-background-networking', '--force-prefers-reduced-motion', '--user-data-dir=' + profile, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const evidence = { phase, checks: [], resources: {}, viewports: [] };
const sockets = [];
const WebSocketClient = process.env.BIZ_UX_WS
  ? (await import(pathToFileURL(process.env.BIZ_UX_WS))).default
  : WebSocket;
async function connect(target) {
  const socket = new WebSocketClient(target.webSocketDebuggerUrl, { perMessageDeflate: false });
  sockets.push(socket);
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', reject, { once: true });
  });
  const pending = new Map();
  const posts = [];
  let sequence = 0;
  socket.addEventListener('message', event => {
    const { id, result, error, method, params } = JSON.parse(event.data);
    if (method === 'Network.requestWillBeSent' && params.request.method === 'POST') {
      posts.push(new URL(params.request.url).pathname);
    }
    pending.get(id)?.(error, result);
  });
  const call = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++sequence;
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(method + ' timed out')); }, 12000);
    pending.set(id, (error, result) => {
      clearTimeout(timer);
      pending.delete(id);
      if (error) reject(new Error(method + ': ' + error.message));
      else resolve(result);
    });
    socket.send(JSON.stringify({ id, method, params }));
  });
  const evaluate = async (expression) => {
    const result = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    assert.ok(!result.exceptionDetails, 'Browser evaluation failed: ' + expression.slice(0, 170));
    return result.result.value;
  };
  const located = async (route) => until(async () => assert.equal(
    await evaluate('location.pathname + "|" + document.readyState'), route + '|complete'));
  const go = async (route) => {
    const url = base + route;
    const previous = await evaluate('performance.timeOrigin');
    const response = await call('Page.navigate', { url });
    assert.ok(!response.errorText, response.errorText);
    await until(async () => {
      assert.equal(await evaluate('location.href + "|" + document.readyState'), url + '|complete');
      if (response.loaderId) assert.notEqual(await evaluate('performance.timeOrigin'), previous);
    });
  };
  await call('Page.enable');
  await call('Network.enable');
  await call('Network.setCacheDisabled', { cacheDisabled: true });
  await call('Network.setBlockedURLs', { urlPatterns: [
    { urlPattern: base + '/*', block: false },
    { urlPattern: 'https://*', block: true },
    { urlPattern: 'http://*', block: true },
  ] });
  return { call, evaluate, go, located, posts };
}
const record = (name, details) => evidence.checks.push({ name, ...details });

try {
  const port = await until(async () => (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split(/\r?\n/)[0]);
  const endpoint = 'http://127.0.0.1:' + port;
  const { Browser } = await (await fetch(endpoint + '/json/version')).json();
  evidence.browser = Browser;
  const [target] = (await (await fetch(endpoint + '/json/list')).json()).filter(item => item.type === 'page');
  const { call, evaluate, go, located, posts } = await connect(target);
  const cssHeaders = new Map();
  const coverageSocket = sockets[0];
  coverageSocket.addEventListener('message', event => {
    const { method, params } = JSON.parse(event.data);
    if (method === 'CSS.styleSheetAdded') cssHeaders.set(params.header.styleSheetId, params.header.sourceURL);
  });
  await call('DOM.enable');
  await call('CSS.enable');
  async function measure(route) {
    await call('CSS.startRuleUsageTracking');
    await go(route);
    const { ruleUsage } = await call('CSS.stopRuleUsageTracking');
    const cssUsage = Object.groupBy(ruleUsage, rule => cssHeaders.get(rule.styleSheetId));
    evidence.resources[route] = await evaluate(`(() => ({
      nodes: document.querySelectorAll('*').length,
      ttfb: Math.round(performance.getEntriesByType('navigation')[0].responseStart),
      resources: performance.getEntriesByType('resource').map(({ name, transferSize, decodedBodySize }) =>
        ({ path: new URL(name).pathname, transferSize, decodedBodySize }))
    }))()`);
    evidence.resources[route].cssRules = Object.fromEntries(Object.entries(cssUsage).filter(([url]) => url.startsWith(base))
      .map(([url, rules]) => [new URL(url).pathname, { tracked: rules.length, used: rules.filter(rule => rule.used).length }]));
  }
  const viewport = (width, height) => call('Emulation.setDeviceMetricsOverride',
    { width, height, deviceScaleFactor: 1, mobile: false });
  const key = async (key, code, windowsVirtualKeyCode) => {
    await call('Input.dispatchKeyEvent', { type: 'keyDown', key, code, windowsVirtualKeyCode,
      ...(key === 'Enter' ? { text: '\r' } : {}) });
    await call('Input.dispatchKeyEvent', { type: 'keyUp', key, code, windowsVirtualKeyCode });
  };
  const capture = async (name) => {
    const { data } = await call('Page.captureScreenshot', { format: 'webp', quality: 78, captureBeyondViewport: false });
    await writeFile(path.resolve('docs/ui/screenshots/BIZ-UX-01', phase + '-' + name + '.webp'), Buffer.from(data, 'base64'));
  };
  const layout = async (route, width, height) => {
    await viewport(width, height);
    await go(route);
    const size = await evaluate(`({ width: innerWidth, scrollWidth: document.documentElement.scrollWidth,
      step: document.querySelector('[data-cart-step]')?.getBoundingClientRect().width ?? null })`);
    evidence.viewports.push({ route, width, height, ...size });
    assert.ok(size.scrollWidth <= width + 1, 'Page overflow: ' + route + ' at ' + width);
  };
  const add = async (variant) => {
    await go('/products/fixture-' + (variant === 1 ? 'a' : 'b'));
    await evaluate(`document.querySelector('form[action="/cart/items"]').requestSubmit()`);
    await located('/cart');
  };
  const cart = () => evaluate(`(() => ({
    quantities: [...document.querySelectorAll('.cart-edit input[name=quantity]')].map(el => +el.value),
    subtotal: document.querySelector('.cart-summary dd')?.textContent.trim(),
    badge: document.querySelector('.cart-count')?.textContent.trim(),
    busy: Boolean(document.querySelector('.cart-line[aria-busy=true]')),
    submitVisible: !document.querySelector('.cart-edit button[type=submit]')?.hidden,
    recovery: Boolean(document.querySelector('[data-cart-recovery]:not([hidden])'))
  }))()`);
  const update = (variant) => evaluate(`document.querySelector('[data-cart-line="${variant}"] [data-cart-step="1"]').click()`);
  const login = async (role) => {
    await evaluate(`document.querySelector('#username').value = '${role}@example.invalid'; document.querySelector('#password').focus()`);
    await call('Input.insertText', { text: process.env.BIZ_UX_FIXTURE_PASS });
    await key('Enter', 'Enter', 13);
  };
  const logout = async () => {
    await evaluate(`document.querySelector('form[action="/logout"]').requestSubmit()`);
    await located('/login');
  };
  const links = () => evaluate(`[...document.querySelectorAll('#home-advisor a.btn, .site-footer a, .site-header a[href="/tu-van-da"]')]
    .map(a => [a.textContent.trim().replace(/\\s+/g, ' '), new URL(a.href).pathname])`);
  const noJsSubmit = async () => {
    await evaluate(`const input = document.querySelector('[data-cart-line="1"] input[name=quantity]');
      input.value = '1'; input.closest('form').requestSubmit()`);
    await until(async () => {
      const refreshed = await cart();
      assert.equal(refreshed.quantities[0], 1);
      assert.equal(refreshed.subtotal.replace(/\D/g, ''), '400000');
    });
  };

  await viewport(1366, 768);
  await measure('/products');
  await go('/?home=opening#home-advisor');
  evidence.guestLinks = await links();
  await evaluate(`document.querySelector('#home-advisor').scrollIntoView({ block: 'center' })`);
  await capture('advisor-1366');
  await viewport(390, 844);
  await evaluate(`document.querySelector('.site-footer').scrollIntoView()`);
  await capture('footer-390');
  if (phase === 'after') {
    assert.ok(evidence.guestLinks.some(([label, route]) => label.startsWith('Hỏi BeautyShop') && route === '/chat'));
    assert.ok(!evidence.guestLinks.some(([label, route]) => label.includes('Chính sách') && ['/products', '/'].includes(route)));
    await go('/chat');
    await go('/tu-van-da');
    record('advisor-and-footer-destinations', { outcome: 'PASS' });
  }
  await add(1);
  await add(2);
  assert.deepEqual((await cart()).quantities, [1, 1]);
  await evaluate(`document.querySelector('.cart-lines').scrollIntoView()`);
  await capture('cart-390');

  // Hold the first response body after the real POST and GET have completed on the server.
  await evaluate(`window.cartQa = { calls: [], release: null };
    const original = window.fetch.bind(window);
    window.fetch = async (...args) => {
      const url = new URL(args[0], location.href);
      if (!url.pathname.endsWith('/quantity')) return original(...args);
      const call = { route: url.pathname, ready: false };
      window.cartQa.calls.push(call);
      const response = await original(...args);
      call.ready = true;
      if (url.pathname.includes('/1/')) {
        await new Promise(resolve => { window.cartQa.release = resolve; });
      }
      return response;
    }`);
  await update(1);
  await until(async () => assert.ok(await evaluate('Boolean(window.cartQa.release)')));
  await update(2);
  await delay(700);
  const beforeRelease = await cart();
  const requestsBeforeRelease = await evaluate('window.cartQa.calls.length');
  await evaluate('window.cartQa.release()');
  await until(async () => assert.equal((await cart()).busy, false));
  const displayed = await cart();
  await go('/cart');
  const server = await cart();
  if (phase === 'before') assert.notEqual(displayed.subtotal, server.subtotal);
  else assert.equal(displayed.subtotal, server.subtotal);
  assert.deepEqual(server.quantities, [2, 2]);
  record('two-sku-out-of-order', { requestsBeforeRelease, beforeRelease, displayed, server });

  // Simulate losing a response after the browser already received a valid server response.
  const lostStart = posts.length;
  await evaluate(`const original = window.fetch.bind(window);
    window.fetch = async (...args) => {
      const response = await original(...args);
      if (String(args[0]).endsWith('/quantity')) throw new TypeError('Fixture response lost');
      return response;
    }`);
  await update(1);
  if (phase === 'before') await until(() => assert.equal(posts.length - lostStart, 2));
  else await until(async () => assert.equal((await cart()).recovery, true));
  const lostView = await cart();
  const lostRequests = posts.length - lostStart;
  assert.equal(lostRequests, phase === 'before' ? 2 : 1);
  record('lost-response-after-commit', { posts: lostRequests, view: lostView });
  if (phase === 'after') await capture('cart-recovery-390');
  await go('/cart');
  assert.deepEqual((await cart()).quantities, [3, 2]);

  // An unanswered request must reach a bounded recovery state.
  const hangStart = posts.length;
  await evaluate(`const original = window.fetch.bind(window);
    window.fetch = (url, options) => String(url).endsWith('/quantity')
      ? new Promise((resolve, reject) => options.signal?.addEventListener('abort', () => reject(options.signal.reason), { once: true }))
      : original(url, options)`);
  await update(1);
  if (phase === 'after') await until(async () => assert.equal((await cart()).recovery, true), 11000);
  else { await delay(8500); assert.equal((await cart()).busy, true); }
  record('hung-request', { posts: posts.length - hangStart, view: await cart() });
  await go('/cart');
  assert.deepEqual((await cart()).quantities, [3, 2]);

  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/cart');
  assert.equal((await cart()).submitVisible, true);
  await noJsSubmit();
  await call('Emulation.setScriptExecutionDisabled', { value: false });
  record('no-js-cart-post', { outcome: 'PASS' });

  await call('Page.navigate', { url: base + '/checkout' });
  await located('/login');
  await login('buyer');
  await located('/checkout');
  const units = await evaluate(`[...document.querySelectorAll('.checkout-items li')].map(item => +item.textContent.match(/×\\s*(\\d+)/)?.[1])`);
  assert.deepEqual(units, [1, 2]);
  for (const [width, height] of [[390, 844], [1366, 768]]) await layout('/checkout', width, height);
  await evaluate(`document.querySelector('[data-update-quote]').click()`);
  await located('/checkout/quote');
  const quote = await evaluate(`Object.fromEntries(['subtotal', 'shipping', 'total'].map(key =>
    [key, document.querySelector('[data-checkout-' + key + ']').textContent.trim()]))`);
  assert.equal(quote.total.replace(/\D/g, ''), '425000');
  await evaluate(`document.querySelector('[data-place-order]').click()`);
  const order = await until(async () => {
    const current = await evaluate('location.pathname');
    assert.match(current, /^\/orders\/\d+$/);
    return current;
  });
  await located(order);
  evidence.customerLinks = await links();
  if (phase === 'after') assert.ok(evidence.customerLinks.some(([label, route]) => label === 'Yêu cầu đổi trả' && route === '/account/returns'));
  record('guest-catalog-sku-cart-login-merge-address-quote-cod-order', { units, quote, order });
  await logout();
  await go('/login');
  await login('admin');
  await located('/admin/dashboard');
  for (const [width, height] of [[390, 844], [1366, 768]]) await layout('/admin/orders', width, height);
  await measure('/admin/orders');
  await go('/admin' + order);
  await evaluate(`document.querySelector('form[action$="/status"] button[value="CONFIRMED"]').click()`);
  await until(async () => assert.ok(await evaluate(`Boolean(document.querySelector('dialog[open]') || document.querySelector('.alert--success'))`)));
  if (await evaluate(`Boolean(document.querySelector('dialog[open]'))`)) {
    await evaluate(`document.querySelector('dialog [data-confirm-accept]').click()`);
  }
  await until(async () => assert.ok(await evaluate(`Boolean(document.querySelector('form[action$="/status"] [name=status][value=PACKING]'))`)));
  record('admin-confirm-cod', { outcome: 'PASS' });
  await logout();
  if (phase === 'after') {
    for (const [width, height] of [[360, 800], [390, 844], [768, 1024], [1366, 768], [1920, 1080]]) {
      await layout('/products', width, height);
      await layout('/cart', width, height);
    }
  }
} catch (error) {
  evidence.error = error.message;
  throw error;
} finally {
  await writeFile(path.resolve('docs/qa/BIZ-UX-01', phase + '-browser.json'), JSON.stringify(evidence, null, 2) + '\n');
  sockets.forEach(socket => socket.close());
  chrome.kill();
}
