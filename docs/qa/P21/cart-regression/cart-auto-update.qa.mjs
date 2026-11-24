// P21 copy of P17 cart QA (via P18), outputs redirected to docs/qa/P21/cart-regression (regression run).
// P17 QA: drives /cart in headless Chrome through the DevTools protocol (no npm dependency).
// Usage: node docs/qa/P17/cart-auto-update.qa.mjs <chrome.exe> <baseUrl> <repoRoot>
// Writes docs/qa/P17/results.json and selected screenshots to docs/ui/screenshots/P17/.
// The throwaway QA customer password is random, kept in memory only and never written to disk.
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';

const [, , chrome, base, repo] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P21/cart-regression');
const shotDir = path.join(repo, 'docs/qa/P21/cart-regression/screenshots');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p17-chrome-'));
const port = 9351;
const proc = spawn(chrome, ['--headless=new', `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`,
  '--no-first-run', '--hide-scrollbars', 'about:blank'], { stdio: 'ignore' });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
for (let i = 0; i < 60; i++) { try { await fetch(`http://127.0.0.1:${port}/json/version`); break; } catch { await sleep(200); } }
const page = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find((t) => t.type === 'page');
const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise((r) => { ws.onopen = r; });
let nextId = 0;
const pending = new Map();
const listeners = [];
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return; }
  if (m.method) for (const l of [...listeners]) if (l.method === m.method) l.fn(m.params);
};
const send = (method, params = {}) => new Promise((r) => { const id = ++nextId; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })); })
  .then((m) => { if (m.error) throw new Error(`${method}: ${m.error.message}`); return m.result; });
const on = (method, fn) => listeners.push({ method, fn });
const ev = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
  return r.result.value;
};
const shot = async (name) => {
  const r = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(path.join(shotDir, `${name}.png`), Buffer.from(r.data, 'base64'));
};
await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
const posts = [];
on('Network.requestWillBeSent', (p) => {
  if (p.request.method === 'POST' && /\/cart\/items\/\d+\/quantity$/.test(p.request.url)) posts.push({ url: p.request.url, type: p.type });
});
const loaded = () => new Promise((r) => { const l = { method: 'Page.loadEventFired', fn: () => { listeners.splice(listeners.indexOf(l), 1); r(); } }; listeners.push(l); });
const go = async (url) => { const l = loaded(); await send('Page.navigate', { url: base + url }); await l; await sleep(300); };
const viewport = (w, h) => send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: 1, mobile: w < 768 });
const waitIdle = async () => {
  for (let i = 0; i < 60; i++) {
    const busy = await ev(`!!document.querySelector('.cart-line[aria-busy="true"]')`);
    if (!busy) return;
    await sleep(100);
  }
  throw new Error('line stayed busy');
};
const click = (selector) => ev(`(() => { const el = document.querySelector(${JSON.stringify(selector)}); el.focus(); el.click(); return true; })()`);
const lineSel = (id) => `[data-cart-line="${id}"]`;
const snapshot = (id) => ev(`(() => {
  const line = document.querySelector('${lineSel(id)}');
  const dd = line ? line.querySelectorAll('.cart-prices dd') : [];
  return { qty: line?.querySelector('input[name=quantity]').value, max: line?.querySelector('input[name=quantity]').max,
    unit: dd[0]?.textContent.trim(), total: dd[1]?.textContent.trim(), stock: line?.querySelector('.cart-stock').textContent.replace(/\\s+/g,' ').trim(),
    error: line?.querySelector('.field-error').textContent.trim(),
    subtotal: document.querySelector('.cart-summary dd')?.textContent.trim(),
    badge: document.querySelector('.site-header a.header-action[href="/cart"] .cart-count')?.textContent.trim() ?? null,
    live: document.querySelector('[data-cart-live]')?.textContent.trim(),
    submitHidden: line?.querySelector('.cart-edit button[type=submit]').hidden,
    plusDisabled: line?.querySelector('[data-cart-step="1"]').disabled,
    focus: document.activeElement?.getAttribute('data-cart-step') ?? document.activeElement?.id ?? document.activeElement?.tagName };
})()`);
const lineIds = () => ev(`[...document.querySelectorAll('[data-cart-line]')].map((l) => l.dataset.cartLine)`);
const compareWithReload = async (ids) => {
  const before = {}; for (const id of ids) before[id] = await snapshot(id);
  await go('/cart');
  const after = {}; for (const id of ids) after[id] = await snapshot(id);
  const keys = ['qty', 'total', 'subtotal', 'badge'];
  const mismatches = ids.flatMap((id) => keys.filter((k) => before[id][k] !== after[id][k]).map((k) => `${id}.${k}: ${before[id][k]} != ${after[id][k]}`));
  return { ok: mismatches.length === 0, mismatches };
};
const addToCart = async (slug) => {
  await go(`/products/${slug}`);
  const l = loaded();
  const ok = await ev(`(() => { const f = document.querySelector('form[action="/cart/items"]'); if (!f) return false; f.requestSubmit(); return true; })()`);
  if (ok) { await l; await sleep(300); }
  return ok;
};

const results = { base, startedAt: new Date().toISOString(), guest: {}, customer: {}, viewports: {}, noJs: {} };
const check = (bucket, name, pass, detail) => { bucket[name] = { pass: !!pass, ...detail }; console.log(`${pass ? 'PASS' : 'FAIL'} ${name}`); };

async function scenario(bucket, label) {
  // Three SKUs: two with stock 12, one with stock 1 (DEMO-004) to exercise the + limit.
  for (const slug of ['demo-r001', 'demo-r002', 'demo-r004']) await addToCart(slug);
  await viewport(1366, 900);
  await go('/cart');
  const ids = await lineIds();
  const [a, b] = ids;
  const others0 = await snapshot(b);
  const s0 = await snapshot(a);
  check(bucket, 'enhanced: submit hidden', s0.submitHidden === true, { s0 });

  posts.length = 0;
  await click(`${lineSel(a)} [data-cart-step="1"]`);
  await waitIdle();
  const s1 = await snapshot(a);
  check(bucket, '+1 updates qty/total/subtotal/badge', Number(s1.qty) === Number(s0.qty) + 1 && s1.total !== s0.total && s1.subtotal !== s0.subtotal && s1.badge !== s0.badge && posts.length === 1,
    { before: s0, after: s1, posts: posts.length });
  check(bucket, 'live region announces', /^Đã cập nhật số lượng SKU .+: \d+\. Thành tiền .+\. Tạm tính .+\.$/.test(s1.live), { live: s1.live });
  check(bucket, 'focus stays on + after update', s1.focus === '1', { focus: s1.focus });
  if (label === 'guest') await shot('guest-after-plus-1366');

  posts.length = 0;
  for (let i = 0; i < 3; i++) await click(`${lineSel(a)} [data-cart-step="1"]`);
  const during = await snapshot(a);
  await waitIdle();
  const s3 = await snapshot(a);
  check(bucket, '+ x3 fast -> one request, final value', posts.length === 1 && Number(s3.qty) === Number(s1.qty) + 3 && Number(during.qty) === Number(s1.qty) + 3,
    { posts: posts.length, during: during.qty, after: s3.qty });
  check(bucket, 'other line untouched', JSON.stringify((({ qty, total }) => ({ qty, total }))(await snapshot(b))) === JSON.stringify((({ qty, total }) => ({ qty, total }))(others0)), {});
  check(bucket, 'matches server after reload (+)', (await compareWithReload(ids)).ok, {});

  // Down to 1.
  posts.length = 0;
  for (let q = Number((await snapshot(a)).qty); q > 1; q--) { await click(`${lineSel(a)} [data-cart-step="-1"]`); await sleep(60); }
  await waitIdle();
  const sDown = await snapshot(a);
  check(bucket, '- down to 1', sDown.qty === '1' && posts.length >= 1, { qty: sDown.qty, posts: posts.length });

  // - at 1 -> dialog, cancel -> back to 1, no request.
  posts.length = 0;
  await click(`${lineSel(a)} [data-cart-step="-1"]`);
  await sleep(200);
  const dialogOpen = await ev(`document.querySelector('[data-confirm-dialog]').open`);
  await click('[data-confirm-cancel]');
  await sleep(300);
  const sCancel = await snapshot(a);
  check(bucket, '- at 1 -> confirm, cancel restores 1 without request', dialogOpen && sCancel.qty === '1' && posts.length === 0 && sCancel.focus === '-1',
    { dialogOpen, qty: sCancel.qty, posts: posts.length, focus: sCancel.focus });
  if (label === 'guest') { await click(`${lineSel(a)} [data-cart-step="-1"]`); await sleep(200); await shot('guest-remove-confirm-1366'); await click('[data-confirm-cancel]'); await sleep(200); }

  // Type a value then Tab (change event).
  posts.length = 0;
  // Real typing: select the field content and insert text so the browser fires input + change on blur.
  await ev(`(() => { const i = document.querySelector('${lineSel(b)} input[name=quantity]'); i.focus(); i.select(); return true; })()`);
  await send('Input.insertText', { text: '4' });
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 });
  await sleep(100); await waitIdle();
  const sTyped = await snapshot(b);
  check(bucket, 'typed 4 + Tab -> saved', sTyped.qty === '4' && posts.length === 1, { qty: sTyped.qty, posts: posts.length });

  // Over stock, client side: max attribute blocks sending.
  posts.length = 0;
  await ev(`(() => { const i = document.querySelector('${lineSel(b)} input[name=quantity]'); i.focus(); i.value = '999'; i.dispatchEvent(new Event('change', {bubbles:true})); return true; })()`);
  await sleep(600);
  const sClient = await snapshot(b);
  check(bucket, 'over stock (client max) -> message, no request', posts.length === 0 && /Nhập số nguyên từ 0 đến/.test(sClient.error), { error: sClient.error, posts: posts.length });

  // Over stock, server side: lift the client max to prove the server message is shown and value restored.
  posts.length = 0;
  await ev(`(() => { const i = document.querySelector('${lineSel(b)} input[name=quantity]'); i.max = '100000'; i.value = '999'; i.dispatchEvent(new Event('input', {bubbles:true})); i.dispatchEvent(new Event('change', {bubbles:true})); return true; })()`);
  await sleep(500); await waitIdle();
  const sServer = await snapshot(b);
  check(bucket, 'over stock (server) -> line error, value restored', posts.length === 1 && sServer.qty === sTyped.qty && sServer.error.length > 0 && Number(sServer.max) < 999,
    { error: sServer.error, qty: sServer.qty, max: sServer.max });
  if (label === 'guest') await shot('guest-server-error-1366');

  // + disabled at stock limit (DEMO-004 has stock 1).
  const limited = [];
  for (const id of await lineIds()) { const s = await snapshot(id); if (s.max === s.qty) limited.push({ id, plusDisabled: s.plusDisabled }); }
  check(bucket, '+ disabled at available stock', limited.length > 0 && limited.every((l) => l.plusDisabled), { limited });

  // Keyboard: Enter on + keeps focus on +.
  posts.length = 0;
  await ev(`document.querySelector('${lineSel(b)} [data-cart-step="1"]').focus()`);
  const beforeKey = await snapshot(b);
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13, text: String.fromCharCode(13) });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  await sleep(500); await waitIdle();
  const sKey = await snapshot(b);
  check(bucket, 'keyboard Enter on + updates and keeps focus', Number(sKey.qty) === Number(beforeKey.qty) + 1 && sKey.focus === '1' && posts.length === 1, { qty: sKey.qty, focus: sKey.focus });
  posts.length = 0;
  await ev(`(() => { const i = document.querySelector('${lineSel(b)} input[name=quantity]'); i.focus(); i.select(); return true; })()`);
  await send('Input.insertText', { text: '3' });
  await ev(`window.__p17NoReload = true`);
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13, text: String.fromCharCode(13) });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Enter', code: 'Enter', windowsVirtualKeyCode: 13 });
  await sleep(500); await waitIdle();
  const sEnter = await snapshot(b);
  const noReload = await ev(`window.__p17NoReload === true`);
  check(bucket, 'Enter in field -> fetch update without reload', sEnter.qty === '3' && posts.length === 1 && posts[0].type === 'Fetch' && noReload && sEnter.focus === `quantity-${b}`,
    { qty: sEnter.qty, posts, noReload, focus: sEnter.focus });
  check(bucket, 'matches server after reload (mixed)', (await compareWithReload(await lineIds())).ok, {});

  // - at 1 then confirm -> line removed via the normal POST flow.
  await ev(`(() => { const i = document.querySelector('${lineSel(a)} input[name=quantity]'); return i.value; })()`);
  await click(`${lineSel(a)} [data-cart-step="-1"]`);
  await sleep(200);
  const l = loaded();
  await click('[data-confirm-accept]');
  await l; await sleep(300);
  const removed = !(await ev(`!!document.querySelector('${lineSel(a)}')`));
  check(bucket, '- at 1 then confirm removes line (PRG)', removed, { flash: await ev(`document.querySelector('.cart-page > .alert')?.textContent.replace(/\\s+/g,' ').trim() ?? null`) });
  return ids;
}

// Guest (session cart).
await scenario(results.guest, 'guest');

// Layout: no horizontal scroll, busy text does not move the row.
for (const [w, h] of [[360, 800], [390, 844], [768, 1024], [1366, 768], [1920, 1080]]) {
  await viewport(w, h);
  await go('/cart');
  const r = await ev(`(() => {
    const line = document.querySelector('.cart-line');
    const box = () => { const b = line.getBoundingClientRect(); return [Math.round(b.height), Math.round(line.querySelector('.cart-controls').getBoundingClientRect().height)]; };
    const before = box(); line.setAttribute('aria-busy', 'true'); const during = box(); const visible = getComputedStyle(line.querySelector('.cart-progress')).visibility; line.removeAttribute('aria-busy');
    return { overflowX: document.documentElement.scrollWidth > innerWidth, before, during, progressVisible: visible };
  })()`);
  results.viewports[`${w}x${h}`] = { pass: !r.overflowX && r.before.join() === r.during.join() && r.progressVisible === 'visible', ...r };
  console.log(`${results.viewports[`${w}x${h}`].pass ? 'PASS' : 'FAIL'} viewport ${w}x${h}`);
  if (w === 390) {
    await ev(`document.querySelector('.cart-line').setAttribute('aria-busy','true')`);
    await shot('guest-busy-390');
    await ev(`document.querySelector('.cart-line').removeAttribute('aria-busy')`);
  }
}

// JavaScript disabled: the "Cập nhật" button and POST + redirect still work.
await viewport(1366, 900);
await send('Emulation.setScriptExecutionDisabled', { value: true });
await go('/cart');
const noJsId = (await ev(`[...document.querySelectorAll('[data-cart-line]')].map((l) => l.dataset.cartLine)`))[0];
const noJsBefore = await ev(`(() => { const l = document.querySelector('${lineSel(noJsId)}'); const b = l.querySelector('.cart-edit button[type=submit]');
  return { visible: !b.hidden && b.getBoundingClientRect().width > 0, steppersHidden: [...l.querySelectorAll('[data-cart-step]')].every((s) => s.hidden), qty: l.querySelector('input[name=quantity]').value }; })()`);
await send('Emulation.setScriptExecutionDisabled', { value: false });
const target = String(Number(noJsBefore.qty) === 1 ? 2 : 1);
await ev(`document.querySelector('${lineSel(noJsId)} input[name=quantity]').value = '${target}'`);
await send('Emulation.setScriptExecutionDisabled', { value: true });
{
  const l = loaded();
  await ev(`(() => { const b = document.querySelector('${lineSel(noJsId)} .cart-edit button[type=submit]'); b.click(); return true; })()`);
  await l; await sleep(300);
}
const noJsAfter = await ev(`({ qty: document.querySelector('${lineSel(noJsId)} input[name=quantity]').value, flash: document.querySelector('.cart-page > .alert')?.textContent.replace(/\\s+/g,' ').trim() ?? null, path: location.pathname })`);
await shot('nojs-after-update-1366');
await send('Emulation.setScriptExecutionDisabled', { value: false });
results.noJs = { pass: noJsBefore.visible && noJsBefore.steppersHidden && noJsAfter.qty === target && /Đã cập nhật số lượng/.test(noJsAfter.flash || '') && noJsAfter.path === '/cart', before: noJsBefore, after: noJsAfter };
console.log(`${results.noJs.pass ? 'PASS' : 'FAIL'} no-JS update`);

// Customer (DB cart): register a throwaway QA account, log in, repeat the scenario.
const email = `qa-p17-${crypto.randomUUID().slice(0, 8)}@example.test`;
const password = crypto.randomBytes(18).toString('base64url');
await go('/register');
{
  const l = loaded();
  await ev(`(() => { const f = document.querySelector('form[action="/register"]'); f.hoTen.value = 'QA P17'; f.email.value = ${JSON.stringify(email)};
    f.matKhau.value = ${JSON.stringify(password)}; const c = f.querySelector('[name="xacNhanMatKhau"]'); if (c) c.value = ${JSON.stringify(password)};
    f.querySelectorAll('input[type=checkbox][required]').forEach((x) => { x.checked = true; }); f.requestSubmit(); return true; })()`);
  await l; await sleep(300);
}
if (await ev(`!!document.querySelector('form[action="/login"]')`)) {
  const l = loaded();
  await ev(`(() => { const f = document.querySelector('form[action="/login"]'); f.username.value = ${JSON.stringify(email)}; f.password.value = ${JSON.stringify(password)}; f.requestSubmit(); return true; })()`);
  await l; await sleep(300);
}
await go('/cart');
const isCustomer = await ev(`!!document.querySelector('a[href="/orders"], a[href="/account"]')`);
results.customer.loggedIn = { pass: isCustomer, account: 'qa-p17-<random>@example.test' };
console.log(`${isCustomer ? 'PASS' : 'FAIL'} customer login`);
if (isCustomer) await scenario(results.customer, 'customer');

results.finishedAt = new Date().toISOString();
const all = [...Object.values(results.guest), ...Object.values(results.customer), ...Object.values(results.viewports), results.noJs];
results.summary = { pass: all.filter((r) => r.pass).length, fail: all.filter((r) => !r.pass).length };
fs.writeFileSync(path.join(qaDir, 'results.json'), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results.summary));
ws.close();
proc.kill();
