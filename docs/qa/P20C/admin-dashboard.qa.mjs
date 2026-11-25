// P20 Admin Chrome harness adapted for the P20C dashboard and sidebar.
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';

const [, , mode, chrome, base, repo] = process.argv;
const fixture = mode === 'layout-fixture';
if ((!fixture && mode !== 'admin') || !chrome || !repo || !['localhost', '127.0.0.1'].includes(new URL(base).hostname)) {
  throw new Error('Usage: node docs/qa/P20C/admin-dashboard.qa.mjs <admin|layout-fixture> <chrome.exe> <local-base-url> <repo-root>');
}
const qaDir = path.join(repo, 'docs/qa/P20C');
const shotDir = path.join(repo, 'docs/ui/screenshots/P20C');
fs.mkdirSync(shotDir, { recursive: true });
const fixtureServer = fixture ? http.createServer((request, response) => {
  const url = new URL(request.url, base);
  const assetRoot = path.join(repo, 'src/main/resources/static');
  let file;
  if (url.pathname === '/admin/dashboard') {
    const range = ['7d', '30d', '90d'].includes(url.searchParams.get('range')) ? url.searchParams.get('range') : '30d';
    file = path.join(repo, 'target/p20c-layout', url.searchParams.has('from') ? 'invalid.html' : range + '.html');
  } else {
    file = path.resolve(assetRoot, '.' + url.pathname);
    if (!file.startsWith(assetRoot + path.sep)) file = null;
  }
  if (!file || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
    response.writeHead(404).end();
    return;
  }
  const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.svg': 'image/svg+xml' };
  response.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream' });
  fs.createReadStream(file).pipe(response);
}) : null;
if (fixtureServer) {
  await new Promise((resolve, reject) => fixtureServer.once('error', reject).listen(Number(new URL(base).port), '127.0.0.1', resolve));
}
const profileRoot = path.join(repo, 'target/p20c-chrome');
fs.mkdirSync(profileRoot, { recursive: true });
const profile = fs.mkdtempSync(path.join(fs.realpathSync(profileRoot), 'admin-'));
const flags = ['--remote-debugging-port=0', '--remote-debugging-address=127.0.0.1',
  `--user-data-dir=${profile}`, '--no-first-run', '--no-default-browser-check', '--hide-scrollbars',
  '--disable-features=PasswordManagerOnboarding,PasswordSaveUpdatePrompts,PasswordLeakDetection'];
if (fixture) flags.push('--headless=new');
// Policies for this ephemeral profile. The harness never reads credentials or captures the login page.
fs.mkdirSync(path.join(profile, 'Default'));
fs.writeFileSync(path.join(profile, 'Default', 'Preferences'), JSON.stringify({
  credentials_enable_service: false, profile: { password_manager_enabled: false }
}));
const proc = spawn(chrome, [...flags, `${base}${fixture ? '/admin/dashboard' : '/login'}`], { stdio: 'ignore' });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const portFile = path.join(profile, 'DevToolsActivePort');
for (let i = 0; i < 60 && !fs.existsSync(portFile); i++) await sleep(200);
if (!fs.existsSync(portFile)) {
  fixtureServer?.close();
  proc.kill();
  throw new Error('Chrome did not expose a DevTools endpoint.');
}
const port = Number(fs.readFileSync(portFile, 'utf8').split(/\r?\n/)[0]);
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
  if (m.method === 'Runtime.exceptionThrown') exceptions.push(m.params.exceptionDetails?.exception?.className || 'Page exception');
  if (m.method) for (const l of [...listeners]) if (l.method === m.method) l.fn(m.params);
};
const send = (method, params = {}) => new Promise((r) => { const id = ++nextId; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })); })
  .then((m) => { if (m.error) throw new Error(`${method}: ${m.error.message}`); return m.result; });
const ev = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
  return r.result.value;
};
const shot = async (name, clip) => {
  const params = { format: 'webp', quality: 88 };
  if (clip) Object.assign(params, { clip: { ...clip, scale: 1 }, captureBeyondViewport: true });
  const result = await send('Page.captureScreenshot', params);
  fs.writeFileSync(path.join(shotDir, `${fixture ? 'fixture-' : ''}${name}.webp`), Buffer.from(result.data, 'base64'));
};
await send('Page.enable');
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
  const summary = document.querySelector('.site-header .admin-menu > summary');
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
    rect: a, leftOfMenu: !!(a && s && (s.w === 0 || a.y < s.y)), leftOfBrand: !!(a && bR && a.x < bR.x),
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


const chartProbe = `[...document.querySelectorAll('.dashboard canvas[data-chart]')].map((canvas) => {
  const chart = window.Chart?.getChart(canvas);
  const table = document.getElementById(canvas.dataset.source);
  const cols = canvas.dataset.chart === 'stacked' ? [1, 2, 3] : [Number(canvas.dataset.column)];
  const rows = [...table.tBodies[0].rows].filter((row) => cols.every((col) => row.cells[col]?.dataset.value !== undefined));
  const short = ['line', 'bar'].includes(canvas.dataset.chart);
  const labels = rows.map((row) => (!short && row.cells[0].dataset.label) || row.cells[0].textContent.trim());
  const match = chart?.data.datasets.length === cols.length
    && JSON.stringify(chart.data.labels) === JSON.stringify(labels)
    && cols.every((col, i) => JSON.stringify(chart.data.datasets[i].data)
      === JSON.stringify(rows.map((row) => Number(row.cells[col].dataset.value))));
  const context = canvas.getContext('2d');
  const pixels = canvas.width && canvas.height ? context.getImageData(0, 0, canvas.width, canvas.height).data : [];
  const hasValues = chart?.data.datasets.some((series) => series.data.some((value) => value > 0));
  let painted = false;
  for (let i = 0; i < pixels.length; i += 4) {
    const gold = pixels[i] === 156 && pixels[i + 1] === 107 && pixels[i + 2] === 31;
    const blue = pixels[i] === 42 && pixels[i + 1] === 120 && pixels[i + 2] === 214;
    if (pixels[i + 3] > 0 && (!hasValues || gold || blue)) {
      painted = true;
      break;
    }
  }
  return { source: canvas.dataset.source, type: canvas.dataset.chart, rows: rows.length, match: !!match,
    ready: canvas.dataset.chartReady === 'true' && chart?.width > 0 && chart?.height > 0 && painted,
    accessible: canvas.getAttribute('role') === 'img' && !!canvas.getAttribute('aria-label')
      && !!document.getElementById(canvas.getAttribute('aria-describedby')),
    tableClosed: !canvas.closest('.chart-card').querySelector('details[data-chart-table]').open };
})`;

const sectionShot = async (name, selector, viewportShot = false) => {
  await ev(`(() => {
    const el = document.querySelector(${JSON.stringify(selector)});
    if (!el) return;
    if (getComputedStyle(el).position === 'fixed') window.scrollTo(0, 0);
    else window.scrollTo(0, el.getBoundingClientRect().top + scrollY - 70);
  })()`);
  await sleep(600);
  if (viewportShot) {
    await shot(name);
    return;
  }
  const clip = await ev(`(() => {
    const el = document.querySelector(${JSON.stringify(selector)});
    if (!el) return null;
    const rect = el.getBoundingClientRect();
    return { x: Math.ceil(rect.left), y: Math.ceil(rect.top + scrollY), width: Math.floor(rect.width),
      height: Math.floor(Math.min(rect.height, innerHeight - rect.top)) };
  })()`);
  if (clip?.width && clip.height) await shot(name, clip);
};

const navProbe = `(() => {
  const nav = [...document.querySelectorAll('.admin-nav')].find((menu) => menu.checkVisibility({ contentVisibilityAuto: true }));
  return { links: nav?.querySelectorAll('a').length, current: nav?.querySelector('[aria-current="page"]')?.getAttribute('href'),
    icons: nav?.querySelectorAll('a svg[aria-hidden=true]').length,
    fixed: getComputedStyle(document.querySelector('.admin-sidebar')).position === 'fixed' };
})()`;

const openMobileMenu = async () => {
  await ev("document.querySelector('.admin-menu summary').focus()");
  await enter();
};

async function admin() {
  if (!fixture) console.log('Waiting for the owner to sign in with their Admin account in Chrome (up to 10 minutes).');
  let landed = false;
  for (let i = 0; i < 600; i++) {
    try {
      if (await ev("location.pathname === '/admin/dashboard' && !!document.querySelector('.dashboard')")) {
        landed = true;
        break;
      }
    } catch { /* navigation in progress */ }
    await sleep(1000);
  }
  check(fixture ? 'MockMvc Dashboard fixture loaded' : 'Admin login lands on Dashboard', landed);
  if (!landed) return;
  await send('Runtime.enable');
  await viewport(1366, 768);
  for (const days of [7, 30, 90]) {
    await go(`/admin/dashboard?range=${days}d`);
    const page = await ev(`({
      days: document.querySelectorAll('#table-daily tbody tr').length,
      current: document.querySelector('.dashboard-range [aria-current=page]')?.textContent.trim(),
      rows: [...document.querySelectorAll('.kpi-grid')].map((row) => row.children.length),
      basis: document.querySelector('.dashboard-basis')?.textContent.includes('Số đơn tính theo ngày đặt.')
    })`);
    check(`${days}-day range and 8 KPI cards`, page.days === days && page.current === `${days} ngày`
      && JSON.stringify(page.rows) === '[4,4]' && page.basis, page);
    const charts = await ev(chartProbe);
    const empty = await ev(`[...document.querySelectorAll('.chart-grid .chart-card')].filter((card) =>
      !card.querySelector('canvas') && card.querySelector('.chart-empty')).map((card) => card.querySelector('table').id)`);
    check(`${days}-day charts or explicit empty states`, charts.length + empty.length + Number(empty.includes('table-daily')) === 9, empty);
    if (days === 90) check('90-day dataset exercises all charts', charts.length === 9, { count: charts.length });
    for (const chart of charts) check(`${days}-day ${chart.source} ${chart.type} matches table`,
      chart.ready && chart.match && chart.tableClosed && chart.accessible, chart);
  }
  await go('/admin/dashboard?from=2026-11-10&to=2026-11-01');
  check('Invalid date range gives a warning', !!await ev("document.querySelector('.dashboard-errors .catalog-error')"));

  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
  for (const [width, height] of [[1366, 900], [1920, 1080], [768, 1024], [390, 844]]) {
    await viewport(width, height);
    await go('/admin/dashboard?range=90d');
    await probeAccount(`Dashboard header and no horizontal overflow at ${width}px`, 'button');
    if (width < 1100) await openMobileMenu();
    const nav = await ev(navProbe);
    check(`Admin navigation at ${width}px`, nav.links === 9 && nav.icons === 9 && nav.current === '/admin/dashboard'
      && nav.fixed === (width >= 1100), nav);
    if (width === 390) {
      await shot('sidebar-mobile-390');
      await openMobileMenu();
      await ev("document.activeElement.blur(); window.scrollTo(0, 0)");
      await shot('dashboard-mobile-390');
    }
    if (width === 1366) {
      await shot('dashboard-1366');
      await sectionShot('primary-charts-1366', '.chart-grid--primary', true);
      await sectionShot('analysis-1366', 'section[aria-labelledby="analysis-title"]');
      await sectionShot('payment-status-1366', 'section[aria-labelledby="chart-payment-status-title"]');
      await sectionShot('sidebar-desktop-1366', '.admin-sidebar');
      const sections = await ev(`(() => {
        // Return reasons and order references can identify private data. Mask the screenshot DOM only.
        if (!${fixture}) {
          for (const el of document.querySelectorAll('#table-returns tbody :is(th, td:nth-child(2)), #table-payment-alerts tbody th')) {
            el.textContent = el.cellIndex === 0 ? '[Mã đơn]' : '[Lý do đã ẩn]';
          }
        }
        return ['return-pending-title', 'payment-alert-title', 'voucher-expiring-title'].map((id) => ({
          id, present: !!document.getElementById(id)?.parentElement.querySelector('table'),
          empty: !!document.getElementById(id)?.parentElement.querySelector('.chart-empty')
        }));
      })()`);
      for (const section of sections) {
        check(`Panel ${section.id} renders rows or an empty state`, section.present || section.empty, section);
        await sectionShot(section.id, `section[aria-labelledby="${section.id}"]`);
      }
      await ev("document.querySelector('[data-kpi=sku-sap-het] a').click()");
      check('Low-stock KPI scrolls to matching list', await ev(`location.hash === '#stock-title'
        && document.querySelector('#stock-title').getBoundingClientRect().top < innerHeight`));
    }
  }

  if (!fixture) {
    await viewport(1366, 768);
    for (const [url, current] of [['/admin/catalog/brands', '/admin/catalog/categories'], ['/admin/catalog/products', '/admin/catalog/products'],
      ['/admin/reviews', '/admin/reviews'], ['/admin/returns', '/admin/returns'], ['/admin/ai', '/admin/ai']]) {
      await go(url);
      const nav = await ev(navProbe);
      check(`Current sidebar item for ${url}`, nav.current === current, nav);
    }
    await go('/admin/orders');
    const detail = await ev("document.querySelector('main a[href^=\"/admin/orders/\"]')?.getAttribute('href') || null");
    if (detail) await go(detail);
    check('Order detail keeps Orders current', !!detail && (await ev(navProbe)).current === '/admin/orders');
  }

  await viewport(390, 844);
  await send('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/admin/dashboard?range=90d');
  const nojs = await ev(`(() => {
    const details = [...document.querySelectorAll('details[data-chart-table]')];
    return { open: details.filter((d) => d.open && d.querySelector('table').getBoundingClientRect().height > 0).length,
      overflow: document.documentElement.scrollWidth > innerWidth,
      blankCanvases: [...document.querySelectorAll('canvas[data-chart]')].filter((c) => c.getBoundingClientRect().height > 0).length };
  })()`);
  check('JS off: all 8 tables visible and canvas placeholders hidden', nojs.open === 8 && !nojs.blankCanvases && !nojs.overflow, nojs);
  await openMobileMenu();
  const nojsNav = await ev(navProbe);
  check('JS off: keyboard opens mobile Admin navigation', nojsNav.links === 9 && nojsNav.current === '/admin/dashboard', nojsNav);
  await send('Emulation.setScriptExecutionDisabled', { value: false });
  if (fixture) return;

  await viewport(1366, 768);
  await go('/admin/dashboard');
  const steps = await focusAccountByKeyboard();
  const label = await ev(labelShown);
  check('Admin logout icon has keyboard focus and a visible label', steps === 2 && label?.clip === 'none' && label.focusVisible);
  const load = loaded();
  await enter();
  await load;
  check('Keyboard logout returns to the Guest header', await ev(`location.pathname + location.search === '/?logout'
    && !!document.querySelector('a.header-account[href="/login"]')`));
}

try {
  await admin();
} catch (error) {
  console.error(error.name);
  check('Harness error', false);
} finally {
  check('No page script exceptions', exceptions.length === 0, { count: exceptions.length });
  results.failures = failures;
  results.finishedAt = new Date().toISOString();
  fs.writeFileSync(path.join(qaDir, `results-${mode}.json`), JSON.stringify(results, null, 2));
  console.log(`${results.checks.length - failures}/${results.checks.length} PASS`);
  process.exitCode = failures ? 1 : 0;
  try { await Promise.race([send('Browser.close'), sleep(1000)]); } catch { proc.kill(); }
  ws.close();
  fixtureServer?.close();
  if (proc.exitCode === null) await Promise.race([new Promise((resolve) => proc.once('exit', resolve)), sleep(2000)]);
  const root = fs.realpathSync(profileRoot);
  const candidate = fs.existsSync(profile) && fs.realpathSync(profile);
  if (candidate && path.dirname(candidate) === root && path.basename(candidate).startsWith('admin-')) {
    try { fs.rmSync(candidate, { recursive: true, force: true }); } catch { /* Chrome may hold files briefly. */ }
  }
}
