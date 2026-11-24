// P20 QA: header account icon (storefront + Admin), Admin login redirect, Dashboard ranges/charts/tables/no-JS/overflow.
// Chrome DevTools protocol only (no npm). Usage:
//   node docs/qa/P20/admin-dashboard.qa.mjs storefront <chrome.exe> <baseUrl> <repoRoot>
//       headless; Guest + a Customer registered through the normal /register form (random password, memory only).
//   node docs/qa/P20/admin-dashboard.qa.mjs admin <chrome.exe> <baseUrl> <repoRoot>
//       opens a VISIBLE Chrome window on /login. The owner signs in with their own Admin account; the script never
//       creates, promotes or reads Admin credentials. It waits for /admin/dashboard, then runs the Admin checks.
import { spawn } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const [, , mode, chrome, base, repo] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P20');
const shotDir = path.join(repo, 'docs/ui/screenshots/P20');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p20-chrome-'));
const port = mode === 'admin' ? 9346 : 9345;
const flags = [`--remote-debugging-port=${port}`, `--user-data-dir=${profile}`, '--no-first-run', '--hide-scrollbars'];
if (mode !== 'admin') flags.unshift('--headless=new');
const proc = spawn(chrome, [...flags, mode === 'admin' ? `${base}/login` : 'about:blank'], { stdio: 'ignore' });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
for (let i = 0; i < 60; i++) { try { await fetch(`http://127.0.0.1:${port}/json/version`); break; } catch { await sleep(200); } }
const target = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find((t) => t.type === 'page');
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r) => { ws.onopen = r; });
let nextId = 0;
const pending = new Map();
const listeners = [];
const exceptions = [];
ws.onmessage = (e) => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return; }
  if (m.method === 'Runtime.exceptionThrown') exceptions.push(m.params.exceptionDetails?.exception?.description || m.params.exceptionDetails?.text);
  if (m.method) for (const l of [...listeners]) if (l.method === m.method) l.fn(m.params);
};
const send = (method, params = {}) => new Promise((r) => { const id = ++nextId; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })); })
  .then((m) => { if (m.error) throw new Error(`${method}: ${m.error.message}`); return m.result; });
const ev = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
  return r.result.value;
};
const shot = async (name, clip, full = false) => {
  let params = { format: 'png' };
  if (clip) params.clip = { ...clip, scale: 1 };
  if (full) {
    const m = await send('Page.getLayoutMetrics');
    params = { format: 'png', captureBeyondViewport: true, clip: { x: 0, y: 0, width: m.cssContentSize.width, height: m.cssContentSize.height, scale: 1 } };
  }
  const r = await send('Page.captureScreenshot', params);
  fs.writeFileSync(path.join(shotDir, `${name}.png`), Buffer.from(r.data, 'base64'));
};
await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
const loaded = () => new Promise((r) => { const l = { method: 'Page.loadEventFired', fn: () => { listeners.splice(listeners.indexOf(l), 1); r(); } }; listeners.push(l); });
const go = async (url, wait = 1000) => { const l = loaded(); await send('Page.navigate', { url: /^https?:/.test(url) ? url : base + url }); await l; await sleep(wait); };
const viewport = (w, h) => send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: 1, mobile: w < 768 });
const key = async (k, code, keyCode, shift = false, text) => {
  const modifiers = shift ? 8 : 0;
  // Buttons activate on the keypress text, so Enter must carry a carriage-return text (links activate on keyDown alone).
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: k, code, windowsVirtualKeyCode: keyCode, modifiers, ...(text ? { text } : {}) });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: k, code, windowsVirtualKeyCode: keyCode, modifiers });
};
const tab = () => key('Tab', 'Tab', 9);
const enter = () => key('Enter', 'Enter', 13, false, String.fromCharCode(13));
const results = { mode, base, startedAt: new Date().toISOString(), checks: [] };
let failures = 0;
const check = (name, pass, detail) => {
  results.checks.push({ name, pass, detail });
  if (!pass) failures++;
  console.log(`${pass ? 'PASS' : 'FAIL'} ${name}${detail ? ' ' + JSON.stringify(detail) : ''}`);
};

// Account control probe: position, size, leftmost, clickable, label behaviour, overflow.
const accountProbe = `(() => {
  // Home fallback (content-height, P18) scrolls to chapter 1 on load; record that, then measure from the top.
  const initialScrollY = Math.round(scrollY); window.scrollTo(0, 0);
  const r = (el) => { if (!el) return null; const b = el.getBoundingClientRect(); return { x: Math.round(b.x), y: Math.round(b.y), w: Math.round(b.width), h: Math.round(b.height), r: Math.round(b.right), b: Math.round(b.bottom) }; };
  const hit = (a, b) => a && b && a.w > 0 && b.w > 0 && a.x < b.r && b.x < a.r && a.y < b.b && b.y < a.b;
  const acc = document.querySelector('.site-header .header-account');
  const summary = document.querySelector('.site-header .header-menu > summary');
  const brand = document.querySelector('.site-header .brand img');
  const cart = document.querySelector('.site-header .nav-shell > .header-action');
  const a = r(acc), s = r(summary), bR = r(brand), c = r(cart);
  let topHit = null;
  if (a && a.w) { const el = document.elementFromPoint(a.x + a.w / 2, a.y + a.h / 2); topHit = !!(el && acc.contains(el)); }
  const label = acc?.querySelector('.header-account-label');
  return {
    w: innerWidth, overflowX: document.documentElement.scrollWidth > innerWidth,
    kind: acc ? acc.tagName.toLowerCase() : null, href: acc?.getAttribute('href') || null,
    form: acc?.closest('form') ? { method: acc.closest('form').method, action: new URL(acc.closest('form').action).pathname, csrf: !!acc.closest('form').querySelector('input[name="_csrf"]') } : null,
    label: label?.textContent.trim() || null, svgHidden: acc?.querySelector('svg')?.getAttribute('aria-hidden') === 'true',
    rect: a, leftOfMenu: !!(a && s && (s.w === 0 || a.x < s.x)), leftOfBrand: !!(a && bR && a.x < bR.x),
    overlaps: [hit(a, s) ? 'menu' : null, hit(a, bR) ? 'brand' : null, hit(a, c) ? 'cart' : null].filter(Boolean),
    initialScrollY, clickable: topHit, carousel: document.querySelector('[data-home-carousel]')?.classList.contains('is-ready') ?? null
  };
})()`;

async function probeAccount(name, expectKind) {
  const p = await ev(accountProbe);
  const pass = p.kind === expectKind && p.rect && p.rect.w >= 44 && p.rect.h >= 44 && p.leftOfMenu && p.leftOfBrand
    && p.overlaps.length === 0 && p.clickable && !p.overflowX && p.svgHidden
    && (expectKind === 'a' ? p.href === '/login' && p.label === 'Đăng nhập' : p.form?.method === 'post' && p.form.action === '/logout' && p.form.csrf && p.label === 'Đăng xuất');
  check(name, pass, p);
  return p;
}

// Focus the account control with the keyboard: Tab from the top of the document (skip link first, then the icon).
async function focusAccountByKeyboard() {
  await ev(`(() => { document.activeElement?.blur(); window.scrollTo(0, 0); document.body.focus(); return true; })()`);
  for (let i = 0; i < 4; i++) {
    await tab();
    const on = await ev(`document.activeElement?.classList.contains('header-account')`);
    if (on) return i + 1;
  }
  return -1;
}

const labelShown = `(() => { const l = document.activeElement?.querySelector('.header-account-label'); if (!l) return null; const cs = getComputedStyle(l); const b = l.getBoundingClientRect();
  return { clip: cs.clipPath, w: Math.round(b.width), focusVisible: document.activeElement.matches(':focus-visible'), outline: getComputedStyle(document.activeElement).outlineStyle }; })()`;

async function storefront() {
  await send('Network.clearBrowserCookies');
  // 1. Guest icon at many widths on home (carousel), products and login.
  for (const [w, h] of [[340, 720], [390, 844], [768, 1024], [1099, 800], [1366, 768], [1920, 1080]]) {
    await viewport(w, h);
    for (const page of ['/', '/products', '/login']) {
      await go(page, page === '/' ? 1600 : 700);
      await probeAccount(`guest icon ${page} ${w}x${h}`, 'a');
    }
  }
  // 2. Keyboard: Tab reaches the icon right after the skip link; label appears; Enter opens /login.
  await viewport(1366, 768);
  await go('/products', 700);
  const steps = await focusAccountByKeyboard();
  const lbl = await ev(labelShown);
  check('guest icon keyboard focus + visible label', steps === 2 && lbl && lbl.clip === 'none' && lbl.w > 20 && lbl.focusVisible && lbl.outline !== 'none', { steps, lbl });
  const a = await ev(accountProbe);
  await shot('storefront-header-guest-focus', { x: 0, y: 0, width: Math.min(1366, Math.max(420, a.rect.r + 260)), height: 120 });
  { const l = loaded(); await enter(); await l; await sleep(500); }
  check('guest icon Enter opens /login', (await ev('location.pathname')) === '/login');
  // Home carousel: icon visible and clickable while the chapter carousel runs.
  await go('/', 1800);
  const home = await ev(accountProbe);
  check('home carousel keeps icon usable', home.carousel === true && home.clickable && home.overlaps.length === 0, home);
  await shot('storefront-home-header-1366', { x: 0, y: 0, width: 1366, height: 140 });

  // 3. Customer: register normally, sign in, logout icon by keyboard.
  const email = `qa-p20-${crypto.randomUUID().slice(0, 8)}@example.test`;
  const password = crypto.randomBytes(18).toString('base64url');
  await go('/register', 600);
  { const l = loaded();
    await ev(`(() => { const f = document.querySelector('form[action="/register"]'); f.hoTen.value = 'QA P20'; f.email.value = ${JSON.stringify(email)};
      f.matKhau.value = ${JSON.stringify(password)}; const c = f.querySelector('[name="xacNhanMatKhau"]'); if (c) c.value = ${JSON.stringify(password)};
      f.querySelectorAll('input[type=checkbox][required]').forEach((x) => { x.checked = true; }); f.requestSubmit(); return true; })()`);
    await l; await sleep(400); }
  await go('/login', 500);
  { const l = loaded();
    await ev(`(() => { const f = document.querySelector('form[action="/login"]'); f.username.value = ${JSON.stringify(email)}; f.password.value = ${JSON.stringify(password)}; f.requestSubmit(); return true; })()`);
    await l; await sleep(600); }
  check('customer login keeps storefront flow (not /admin)', !(await ev('location.pathname')).startsWith('/admin'), { path: await ev('location.pathname') });
  for (const [w, h] of [[340, 720], [390, 844], [768, 1024], [1366, 768], [1920, 1080]]) {
    await viewport(w, h);
    for (const page of ['/', '/products', '/account']) {
      await go(page, page === '/' ? 1600 : 700);
      await probeAccount(`customer logout icon ${page} ${w}x${h}`, 'button');
    }
  }
  await viewport(1366, 768);
  await go('/products', 700);
  const cSteps = await focusAccountByKeyboard();
  const cLbl = await ev(labelShown);
  check('customer logout icon keyboard focus + visible label', cSteps === 2 && cLbl?.clip === 'none' && cLbl.focusVisible, { steps: cSteps, cLbl });
  const b = await ev(accountProbe);
  await shot('storefront-header-customer-logout-focus', { x: 0, y: 0, width: Math.min(1366, Math.max(420, b.rect.r + 260)), height: 120 });
  { const l = loaded(); await enter(); await l; await sleep(600); }
  const after = await ev(`({ path: location.pathname + location.search, guestIcon: !!document.querySelector('a.header-account[href="/login"]') })`);
  check('customer keyboard logout (POST + CSRF) returns guest header', after.path === '/?logout' && after.guestIcon, after);
}

const chartProbe = `(() => [...document.querySelectorAll('.dashboard canvas[data-chart]')].map((c) => {
  const chart = window.Chart?.getChart(c);
  const t = document.getElementById(c.dataset.source);
  const col = Number(c.dataset.column);
  const rows = [...t.tBodies[0].rows].filter((row) => row.cells[col]?.dataset.value !== undefined);
  const table = rows.map((row) => Number(row.cells[col].dataset.value));
  const labels = rows.map((row) => row.cells[0].textContent.trim());
  const data = chart ? chart.data.datasets[0].data.map(Number) : null;
  const card = c.closest('.chart-card');
  return { source: c.dataset.source, type: c.dataset.chart, ready: c.dataset.chartReady === 'true', rows: table.length,
    match: !!data && data.length === table.length && data.every((v, i) => v === table[i]) && chart.data.labels.length === labels.length,
    tableClosed: !card.querySelector('details[data-chart-table]').open, title: card.querySelector('h2').textContent.trim(),
    describedBy: !!document.getElementById(c.getAttribute('aria-describedby')), sum: table.reduce((a, b) => a + b, 0) };
}))()`;

async function admin() {
  console.log('Waiting for the owner to sign in with their Admin account in the opened Chrome window (up to 10 minutes)…');
  let landed = null;
  for (let i = 0; i < 600; i++) {
    try {
      const p = await ev('location.pathname');
      if (p === '/admin/dashboard') { landed = p; break; }
    } catch { /* navigation in progress */ }
    await sleep(1000);
  }
  check('admin login lands on /admin/dashboard', landed === '/admin/dashboard', { landed });
  if (!landed) return;
  await sleep(1500);
  // Ranges.
  for (const range of ['7d', '30d', '90d']) {
    await go(`/admin/dashboard?range=${range}`, 1500);
    const s = await ev(`({ current: document.querySelector('.dashboard-range a[aria-current=page]')?.textContent.trim(), period: document.querySelector('.dashboard-period')?.textContent.trim(),
      days: document.querySelectorAll('#table-daily tbody tr').length, kpis: document.querySelectorAll('.kpi-card').length, errors: document.querySelectorAll('.dashboard-errors p').length })`);
    const days = Number(range.replace('d', ''));
    check(`range ${range}`, s.current === `${days} ngày` && s.days === days && s.kpis === 6 && s.errors === 0, s);
    const charts = await ev(chartProbe);
    for (const c of charts) check(`chart ${range} ${c.source} matches table`, c.ready && c.match && c.tableClosed && c.describedBy, c);
    results[`charts_${range}`] = charts;
  }
  // Custom range + validation message.
  await go('/admin/dashboard?from=2026-11-10&to=2026-11-01', 1000);
  check('invalid from>to shows message, no 500', (await ev(`document.querySelector('.dashboard-errors')?.textContent || ''`)).includes('Ngày bắt đầu phải trước'));
  await go('/admin/dashboard?range=30d', 1500);
  // Overflow + screenshots per viewport.
  for (const [w, h] of [[1366, 768], [1920, 1080], [768, 1024], [390, 844]]) {
    await viewport(w, h);
    await go('/admin/dashboard?range=90d', 1800);
    const o = await ev(`({ overflowX: document.documentElement.scrollWidth > innerWidth, sw: document.documentElement.scrollWidth })`);
    check(`dashboard no horizontal scroll ${w}`, !o.overflowX, o);
    await probeAccount(`admin logout icon ${w}`, 'button');
    if (w === 1366 || w === 390) await shot(`dashboard-${w}`, null, true);
  }
  // Each chart card.
  await viewport(1366, 768);
  await go('/admin/dashboard?range=90d', 1800);
  const cards = await ev(`[...document.querySelectorAll('.chart-grid .chart-card')].map((c) => { const b = c.getBoundingClientRect(); return { id: c.querySelector('canvas')?.dataset.source || c.querySelector('table').id, x: b.x, y: b.y + scrollY, w: b.width, h: b.height }; })`);
  for (const c of cards) await shot(`chart-${c.id.replace('table-', '')}`, { x: c.x, y: c.y, width: c.w, height: c.h });
  // Admin header: Tổng quan first, aria-current, keyboard focus label on logout icon.
  const nav = await ev(`({ first: document.querySelector('.nav-links > a')?.textContent.trim(), current: document.querySelector('.nav-links a[aria-current=page]')?.textContent.trim() })`);
  check('admin nav: Tổng quan first + aria-current', nav.first === 'Tổng quan' && nav.current === 'Tổng quan', nav);
  const aSteps = await focusAccountByKeyboard();
  const aLbl = await ev(labelShown);
  check('admin logout icon keyboard focus + visible label', aSteps === 2 && aLbl?.clip === 'none' && aLbl.focusVisible, { steps: aSteps, aLbl });
  const acc = await ev(accountProbe);
  await shot('admin-header-logout-focus', { x: 0, y: 0, width: Math.min(1366, Math.max(420, acc.rect.r + 300)), height: 120 });
  // JS off: tables visible.
  await send('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/admin/dashboard?range=90d', 1200);
  const nojs = await ev(`({ open: [...document.querySelectorAll('details[data-chart-table]')].every((d) => d.open), tables: [...document.querySelectorAll('.chart-table table')].filter((t) => t.getBoundingClientRect().height > 0).length, total: document.querySelectorAll('.chart-table table').length })`).catch(() => null);
  // Runtime.evaluate still works with page scripts disabled.
  check('JS off: every data table visible', nojs && nojs.open && nojs.tables === nojs.total && nojs.total === 6, nojs);
  await shot('dashboard-nojs-1366', { x: 0, y: 0, width: 1366, height: 1400 });
  await send('Emulation.setScriptExecutionDisabled', { value: false });
  // Finally: keyboard logout from Admin.
  await go('/admin/dashboard', 1200);
  await focusAccountByKeyboard();
  { const l = loaded(); await enter(); await l; await sleep(600); }
  const out = await ev(`({ path: location.pathname + location.search, guestIcon: !!document.querySelector('a.header-account[href="/login"]') })`);
  check('admin keyboard logout returns guest header', out.path === '/?logout' && out.guestIcon, out);
}

try {
  if (mode === 'admin') await admin(); else await storefront();
} catch (error) {
  check('harness error', false, String(error?.stack || error));
} finally {
  results.exceptions = exceptions;
  results.failures = failures;
  results.finishedAt = new Date().toISOString();
  fs.writeFileSync(path.join(qaDir, `results-${mode}.json`), JSON.stringify(results, null, 2));
  console.log(`${failures === 0 ? 'ALL PASS' : failures + ' FAIL'}; page exceptions: ${exceptions.length}`);
  ws.close();
  proc.kill();
  try { fs.rmSync(profile, { recursive: true, force: true }); } catch { /* Chrome may hold files briefly */ }
}
