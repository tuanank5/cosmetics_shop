import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const [origin, revision, runtime] = process.argv.slice(2);
assert.equal(new URL(origin).hostname, '127.0.0.1');
assert.ok(['before', 'after'].includes(revision));
const qa = path.resolve('docs/qa/ADMIN-UX-BIZ-01');
const screenshots = path.resolve('docs/ui/screenshots/ADMIN-UX-BIZ-01');
const profile = path.join(runtime, 'chrome-loopback');
const after = revision === 'after';
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
const evidence = { revision, engine: 'Chrome headless', scope: 'Loopback fixture; external network blocked', layout: [], checks: {} };
const browser = spawn(process.env.ADMIN_UX_CHROME || 'C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless', '--no-sandbox', '--disable-gpu', '--disable-background-networking', '--no-first-run', '--lang=vi-VN',
  '--no-default-browser-check', '--no-proxy-server', '--remote-debugging-address=127.0.0.1',
  '--remote-debugging-port=0', '--host-resolver-rules=MAP * ~NOTFOUND, EXCLUDE 127.0.0.1',
  '--user-data-dir=' + profile, '--disk-cache-dir=' + path.join(runtime, 'chrome-cache'), 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
let ws;
let browserError;
let step = 'connect';
browser.once('error', error => { browserError = error; });

async function until(check, ms = 6000) {
  const deadline = Date.now() + ms;
  do {
    if (await check()) return;
    await wait(100);
  } while (Date.now() < deadline);
  throw new Error('Timed out');
}

try {
  let port;
  await until(async () => {
    if (browserError) throw browserError;
    if (browser.exitCode !== null) throw new Error('Chrome exited');
    try { [port] = (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split(/\r?\n/); }
    catch { return false; }
    return !!port;
  });
  const tabs = await (await fetch('http://127.0.0.1:' + port + '/json/list', { signal: AbortSignal.timeout(5000) })).json();
  ws = new WebSocket(tabs.find(tab => tab.type === 'page').webSocketDebuggerUrl);
  await until(() => ws.readyState === WebSocket.OPEN);
  const listeners = new Map();
  let messageId = 0;
  ws.addEventListener('message', ({ data }) => {
    const response = JSON.parse(data);
    listeners.get(response.id)?.(response);
  });
  function send(method, params = {}) {
    const id = ++messageId;
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        listeners.delete(id);
        reject(new Error(method + ' timed out'));
      }, 7000);
      listeners.set(id, response => {
        clearTimeout(timeout);
        listeners.delete(id);
        if (response.error) reject(new Error(method + ' failed'));
        else resolve(response.result);
      });
      ws.send(JSON.stringify({ id, method, params }));
    });
  }
  async function evaluate(expression) {
    const response = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (response.exceptionDetails) throw new Error('DOM expression failed');
    return response.result.value;
  }
  async function size(width, zoom = 1) {
    const now = Math.floor(width / zoom);
    await send('Emulation.setDeviceMetricsOverride', { width: now, height: Math.floor(900 / zoom), deviceScaleFactor: zoom, mobile: false });
    await until(async () => (await evaluate('window.innerWidth')) === now);
  }
  async function visit(route) {
    const response = await send('Page.navigate', { url: origin + route });
    assert.ok(!response.errorText);
    await until(async () => evaluate(
      'document.readyState === "complete" && location.pathname === ' + JSON.stringify(route.split('?')[0])
    ), 10000);
  }
  async function capture(name, selector) {
    await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1030, deviceScaleFactor: 1, mobile: false });
    await until(async () => (await evaluate('window.innerHeight')) === 1030);
    let clip;
    if (selector) {
      await evaluate('document.querySelector(' + JSON.stringify(selector) + ').scrollIntoView()');
      await evaluate('document.fonts.ready.then(() => true)');
      clip = await evaluate(`(() => {
        const r = document.querySelector(${JSON.stringify(selector)}).getBoundingClientRect();
        return {x: Math.max(0, r.x + scrollX), y: Math.max(0, r.y + scrollY),
          width: Math.min(r.width, innerWidth), height: r.height, scale: 1};
      })()`);
    }
    const { data } = await send('Page.captureScreenshot', {
      format: 'webp', quality: 83, captureBeyondViewport: !!clip, ...(clip ? { clip } : {}),
    });
    await writeFile(path.join(screenshots, revision + '-' + name + '.webp'), Buffer.from(data, 'base64'));
  }
  async function key(key, code, windowsVirtualKeyCode) {
    const spec = { key, code, windowsVirtualKeyCode, nativeVirtualKeyCode: windowsVirtualKeyCode };
    await send('Input.dispatchKeyEvent', key === 'Enter' ? { ...spec, type: 'keyDown', text: '\r', unmodifiedText: '\r' }
      : { ...spec, type: 'rawKeyDown' });
    await send('Input.dispatchKeyEvent', { ...spec, type: 'keyUp' });
  }

  await mkdir(screenshots, { recursive: true });
  await send('Page.enable');
  await size(1440);
  step = 'footer';
  await visit('/products');
  await capture('footer', 'footer');
  if (after) {
    const iframe = await evaluate(`(() => {
      const map = document.querySelector('footer iframe');
      return { source: new URL(map.src).hostname, height: map.clientHeight, lazy: map.loading,
        title: map.title.length > 0, fallback: !!document.querySelector('.footer-map-link') };
    })()`);
    assert.deepEqual(iframe, { source: 'www.google.com', height: 238, lazy: 'lazy', title: true, fallback: true });
    evidence.checks.mapIframe = iframe;
  }
  step = 'login';
  await visit('/login');
  await evaluate(`(() => {
    const form = document.querySelector('form[action="/login"]');
    form.elements.username.value = 'admin@example.invalid';
    form.elements.password.value = ${JSON.stringify(process.env.ADMIN_UX_FIXTURE_PASSWORD)};
    form.requestSubmit();
    return true;
  })()`);
  await until(() => evaluate('document.readyState === "complete" && location.pathname !== "/login"'), 10000);
  const routes = {
    orders: '/admin/orders?method=COD',
    detail: '/admin/orders/1?listMethod=COD&listPage=0',
    catalog: '/admin/catalog/products',
    variants: '/admin/catalog/products/1/variants',
    form: '/admin/catalog/products/1/variants/1/edit',
  };

  step = 'screenshots';
  for (const [name, route] of Object.entries(routes)) {
    if (!after && ['variants', 'form'].includes(name)) continue;
    await visit(route);
    assert.equal(await evaluate('document.querySelectorAll("main h1").length'), 1, name);
    await capture(name);
  }
  if (after) {
    step = 'responsive';
    for (const [name, route] of Object.entries({ footer: '/products', ...routes })) {
      await visit(route);
      const dims = [320, 360, 390, 768, 1024, 1280, 1440, 1920].map(width => [width, 1]);
      dims.push([1280, 2], [1440, 2], [1920, 2]);
      for (const [width, zoom] of dims) {
        await size(width, zoom);
        const data = await evaluate(`(() => {
          const root = document.documentElement;
          const table = document.querySelector('.admin-table-scroll');
          const iframe = document.querySelector('footer iframe');
          return {viewport: innerWidth, document: root.scrollWidth, tableContained: !table || table.getBoundingClientRect().right <= innerWidth,
            tableScrollable: !!table && table.scrollWidth > table.clientWidth,
            mapContained: !iframe || iframe.getBoundingClientRect().right <= innerWidth };
        })()`);
        evidence.layout.push({ name, width, visualScale: zoom, ...data });
        assert.ok(data.document <= data.viewport && data.tableContained && data.mapContained, name + ' ' + width);
      }
    }
    step = 'accessible table';
    await size(390);
    await visit(routes.detail);
    assert.equal(await evaluate('document.querySelector(".admin-order-layout").children.length'), 2);
    const ax = await send('Accessibility.getFullAXTree');
    for (const role of ['table', 'row', 'rowheader', 'columnheader']) assert.ok(ax.nodes.some(node => node.role?.value === role));
    assert.ok(ax.nodes.some(node => node.role?.value === 'region' && node.name?.value === 'Sản phẩm trong đơn'));
    step = 'keyboard tab and focus';
    await key('Tab', 'Tab', 9);
    await evaluate('document.querySelector(".admin-table-scroll").focus()');
    const focus = await evaluate(`(() => {
      const active = document.activeElement;
      const rules = getComputedStyle(active);
      return {className: active.className, visible: active.matches(':focus-visible'), outline: rules.outlineStyle};
    })()`);
    assert.deepEqual(focus, { className: 'admin-table-scroll', visible: true, outline: 'solid' });
    await key('Tab', 'Tab', 9);
    assert.equal(await evaluate('document.activeElement.name'), 'status');
    step = 'Enter opens confirmation';
    await key('Tab', 'Tab', 9);
    assert.equal(await evaluate('document.activeElement.value'), 'CANCELLED');
    assert.equal(await evaluate('typeof document.querySelector("[data-confirm-dialog]").showModal'), 'function');
    await key('Enter', 'Enter', 13);
    await until(() => evaluate('!!document.querySelector("dialog[open]")'));
    assert.equal(await evaluate('document.activeElement.hasAttribute("data-confirm-cancel")'), true);
    step = 'Escape restores focus';
    await key('Escape', 'Escape', 27);
    await until(() => evaluate('!document.querySelector("dialog[open]")'));
    assert.equal(await evaluate('document.activeElement.value'), 'CANCELLED');
    step = 'disabled catalog control and SKU preview';
    await size(1440);
    await visit(routes.variants);
    assert.equal(await evaluate(`document.getElementById('sku-block-1').closest('td').querySelector('button').disabled`), true);
    await visit(routes.form);
    const { root } = await send('DOM.getDocument');
    const { nodeId } = await send('DOM.querySelector', { nodeId: root.nodeId, selector: '[data-sku-upload]' });
    await send('DOM.setFileInputFiles', { nodeId, files: [path.join(runtime, 'uploads', 'aa123456-1234-1234-1234-123456789abc.png')] });
    await until(() => evaluate(`(() => {
      const img = document.querySelector('[data-sku-preview]');
      return !img.hidden && img.naturalWidth > 0;
    })()`));
    evidence.checks.keyboard = { tab: true, enter: true, escape: true, focusVisible: true, axTable: true, skuDisabled: true, preview: true };
  }
  evidence.status = 'PASS';
} catch (error) {
  evidence.status = 'FAIL';
  evidence.failedStep = step;
  evidence.failedLine = error.stack?.match(/browser-check\.mjs:(\d+)/)?.[1];
  evidence.error = error.message?.slice(0, 320);
  console.error('Chrome QA failed at %s: %s', step, evidence.error);
  process.exitCode = 1;
} finally {
  ws?.close();
  browser.kill();
  await writeFile(path.join(qa, revision + '-browser.json'), JSON.stringify(evidence, null, 2) + '\n');
}
