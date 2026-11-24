// P21 QA: product sections below the six-chapter carousel. Headless Chrome via DevTools protocol (no npm).
// Usage: node docs/qa/P21/home-products.qa.mjs <chrome.exe> <baseUrl> <repoRoot>
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const [, , chrome, base, repo] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P21');
const shotDir = path.join(repo, 'docs/ui/screenshots/P21');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p21-chrome-'));
const port = 9347;
const proc = spawn(chrome, ['--headless=new', `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`,
  '--no-first-run', '--hide-scrollbars', 'about:blank'], { stdio: 'ignore' });
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
const shot = async (name, clip) => {
  const r = await send('Page.captureScreenshot', { format: 'png', ...(clip ? { clip: { ...clip, scale: 1 } } : {}) });
  fs.writeFileSync(path.join(shotDir, `${name}.png`), Buffer.from(r.data, 'base64'));
};
await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
const loaded = () => new Promise((r) => { const l = { method: 'Page.loadEventFired', fn: () => { listeners.splice(listeners.indexOf(l), 1); r(); } }; listeners.push(l); });
// Always a full document load: blank first so re-opening the same URL#hash is not a same-document jump.
const go = async (url, wait = 1600) => {
  await send('Page.navigate', { url: 'about:blank' }); await sleep(150);
  const l = loaded(); await send('Page.navigate', { url: /^https?:/.test(url) ? url : base + url });
  await Promise.race([l, sleep(15000)]); await sleep(wait);
};
const viewport = (w, h, dpr = 1) => send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: dpr, mobile: w < 768 });
const key = async (k, code, keyCode, shift = false) => {
  const modifiers = shift ? 8 : 0;
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: k, code, windowsVirtualKeyCode: keyCode, modifiers });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: k, code, windowsVirtualKeyCode: keyCode, modifiers });
};
const results = { base, startedAt: new Date().toISOString(), checks: [] };
let failures = 0;
const check = (name, pass, detail) => {
  results.checks.push({ name, pass, detail });
  if (!pass) failures++;
  console.log(`${pass ? 'PASS' : 'FAIL'} ${name}${detail ? ' ' + JSON.stringify(detail) : ''}`);
};

const layout = `(() => {
  const cols = (sel) => { const g = document.querySelector(sel); return g ? getComputedStyle(g).gridTemplateColumns.split(' ').filter(Boolean).length : 0; };
  const top = (id) => { const el = document.getElementById(id); return el ? Math.round(el.getBoundingClientRect().top + scrollY) : null; };
  const root = document.querySelector('[data-home-carousel]');
  return {
    w: innerWidth, overflowX: document.documentElement.scrollWidth > innerWidth,
    mode: root.classList.contains('is-ready') ? 'carousel' : 'fallback (' + (root.dataset.carouselFallback || 'no-js') + ')',
    popularCards: document.querySelectorAll('#home-popular .store-card').length, allCards: document.querySelectorAll('#home-all-products .store-card').length,
    popularCols: cols('.home-popular-grid'), allCols: cols('.home-all-grid'),
    // Document order: end of the six-chapter stage (Swiper transforms the slides, so a chapter's own rect is not a position).
    order: [(() => { const st = document.getElementById('home-stage').getBoundingClientRect(); return Math.round(st.bottom + scrollY); })(),
      top('home-products'), top('home-popular'), top('home-all-products'), top('home-footer')],
    advisorBeforeProducts: !!(document.getElementById('home-advisor').compareDocumentPosition(document.getElementById('home-products')) & Node.DOCUMENT_POSITION_FOLLOWING),
    nextHref: document.querySelector('#home-advisor .story-next')?.getAttribute('href'),
    popularLinks: [...document.querySelectorAll('#home-popular .store-card')].map((a) => a.getAttribute('href'))
  };
})()`;
const state = `(() => { const r = document.querySelector('[data-home-carousel]'); const p = document.getElementById('home-products').getBoundingClientRect();
  const f = document.querySelector('.site-footer').getBoundingClientRect();
  return { slide: r.dataset.activeSlide ? Number(r.dataset.activeSlide) : null, y: Math.round(scrollY), productsInView: p.top < innerHeight && p.bottom > 0,
    footerInView: f.top < innerHeight, atBottom: Math.ceil(scrollY + innerHeight) >= document.documentElement.scrollHeight - 2 }; })()`;

async function wheelFlow(w, h) {
  const seq = [];
  if ((await ev(state)).slide !== 1) { await ev(`document.querySelectorAll('.home-carousel-pagination button')[0].click()`); await sleep(1300); }
  const wheel = async (dy) => {
    await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: Math.floor(w / 2), y: Math.floor(h / 2), deltaX: 0, deltaY: dy });
    await sleep(1100);
    const s = await ev(state);
    seq.push(s);
    return s;
  };
  for (let i = 0; i < 5; i++) await wheel(400);
  let reachedProducts = false, reachedFooter = false;
  for (let i = 0; i < 40; i++) {
    const s = await wheel(600);
    if (s.productsInView && s.y > 0) reachedProducts = true;
    if (s.footerInView && s.atBottom) { reachedFooter = true; break; }
  }
  let backTo6 = false;
  for (let i = 0; i < 60; i++) {
    const s = await wheel(-600);
    if (s.y <= 2 && s.slide === 6) { backTo6 = true; break; }
  }
  const ups = [];
  for (let i = 0; i < 6; i++) { const s = await wheel(-400); if (ups.at(-1) !== s.slide) ups.push(s.slide); }
  const down = seq.slice(0, 5).map((s) => s.slide).join(',');
  return { down, reachedProducts, reachedFooter, backTo6, ups: ups.join(','),
    pass: down === '2,3,4,5,6' && reachedProducts && reachedFooter && backTo6 && ups.join(',') === '5,4,3,2,1' };
}

// 1. Viewports (+ zoom 200% = 1366x768 at DPR 2 => 683 CSS px).
const vps = [[360, 800, 1, '360x800'], [390, 844, 1, '390x844'], [768, 1024, 1, '768x1024'], [1366, 768, 1, '1366x768'],
  [1920, 1080, 1, '1920x1080'], [683, 384, 2, '1366x768-zoom200']];
for (const [w, h, dpr, name] of vps) {
  await viewport(w, h, dpr);
  await go('/', 2000);
  const s = await ev(layout);
  const wantPopular = w >= 1100 ? 5 : w >= 380 ? 2 : 1;
  const wantAll = w >= 1100 ? 4 : w >= 380 ? 2 : 1;
  const ordered = s.advisorBeforeProducts && s.order.every((v, i, a) => v !== null && (i === 0 || v >= a[i - 1] - 1));
  check(`layout ${name} ${s.mode}`, !s.overflowX && s.popularCards === 5 && s.allCards === 12 && s.popularCols === wantPopular
    && s.allCols === wantAll && ordered && s.nextHref === '#home-products', s);
  if (s.mode === 'carousel') {
    const flow = await wheelFlow(w / 1, h / 1);
    check(`wheel ${name} 1→6→products→footer→6→1`, flow.pass, flow);
  }
  if (name === '1366x768' || name === '390x844' || name === '768x1024') {
    await ev(`document.getElementById('home-products').scrollIntoView({ block: 'start' })`);
    await sleep(700);
    await shot(`home-products-${name}`);
    await ev(`document.getElementById('home-all-products').scrollIntoView({ block: 'start' })`);
    await sleep(700);
    await shot(`home-all-products-${name}`);
  }
}

// 2. Chapter-6 anchor, sort links and return to chapter 6.
await viewport(1366, 768);
await go('/', 2000);
await ev(`document.querySelectorAll('.home-carousel-pagination button')[5].click()`);
await sleep(1400);
await ev(`document.querySelector('#home-advisor .story-next').click()`);
await sleep(900);
const anchor = await ev(`({ hash: location.hash, top: Math.round(document.getElementById('home-products').getBoundingClientRect().top) })`);
check('chapter 6 "Thông tin cửa hàng ↓" goes to product area', anchor.hash === '#home-products' && Math.abs(anchor.top) <= 4, anchor);
for (const [label, sort] of [['Giá tăng', 'priceAsc'], ['Giá giảm', 'priceDesc'], ['Mới nhất', 'newest']]) {
  await go('/', 1800);
  const href = await ev(`[...document.querySelectorAll('.home-sort a')].find((a) => a.textContent.trim() === ${JSON.stringify(label)}).getAttribute('href')`);
  await go(href, 2200);
  const s = await ev(`(() => { const prices = [...document.querySelectorAll('#home-all-products .store-card-price .money')].map((m) => Number(m.textContent.replace(/[^0-9]/g, '')));
    const sec = document.getElementById('home-all-products').getBoundingClientRect();
    return { url: location.pathname + location.search + location.hash, current: document.querySelector('.home-sort a[aria-current]')?.textContent.trim(),
      slide: document.querySelector('[data-home-carousel]').dataset.activeSlide, sectionTop: Math.round(sec.top), prices,
      more: document.querySelector('.home-all-more a')?.getAttribute('href') }; })()`);
  const sorted = sort === 'priceAsc' ? s.prices.every((p, i, a) => i === 0 || p >= a[i - 1])
    : sort === 'priceDesc' ? s.prices.every((p, i, a) => i === 0 || p <= a[i - 1]) : true;
  check(`sort ${sort} keeps reader at section, carousel waits on chapter 6`, s.url === `/?sort=${sort}#home-all-products` && s.current === label
    && s.slide === '6' && Math.abs(s.sectionTop - 24) <= 30 && s.prices.length === 12 && sorted
    && s.more === (sort === 'newest' ? '/products' : `/products?sort=${sort}`), s);
  if (sort === 'priceAsc') {
    let back = null;
    for (let i = 0; i < 30; i++) {
      await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: 683, y: 384, deltaX: 0, deltaY: -600 });
      await sleep(900);
      const st = await ev(state);
      if (st.y <= 2) { back = st; break; }
    }
    check('scroll up from sorted list returns to chapter 6', back?.slide === 6, back);
    await go(href, 2200);
    await shot('home-sort-priceAsc-1366');
  }
}

// 3. Keyboard: Tab never lands in hidden chapters; from chapter 6 it continues into the product area.
await go('/', 2000);
await ev(`document.querySelectorAll('.home-carousel-pagination button')[5].click()`);
await sleep(1400);
await ev(`document.querySelector('#home-advisor a.btn').focus()`);
let leaked = [], reached = null;
for (let i = 0; i < 25; i++) {
  await key('Tab', 'Tab', 9);
  const f = await ev(`(() => { const a = document.activeElement; const slide = a.closest('.home-slide');
    return { tag: a.tagName, text: (a.textContent || a.getAttribute('aria-label') || '').trim().slice(0, 40), hiddenSlide: !!(slide && (slide.inert || slide.getAttribute('aria-hidden') === 'true')),
      inProducts: !!a.closest('#home-products'), href: a.getAttribute('href') }; })()`);
  if (f.hiddenSlide) leaked.push(f);
  if (f.inProducts && !reached) reached = { step: i + 1, ...f };
}
check('Tab from chapter 6 reaches product area without entering hidden chapters', leaked.length === 0 && !!reached, { reached, leaked });
await ev(`document.activeElement.scrollIntoView({ block: 'center' })`);
await sleep(300);
const focusRect = await ev(`(() => { const b = document.activeElement.getBoundingClientRect(); return { x: Math.max(0, b.x - 24), y: Math.max(0, b.y - 24), width: Math.min(innerWidth, b.width + 48), height: Math.min(innerHeight, b.height + 48) }; })()`);
await shot('home-products-keyboard-focus', focusRect);
// Shift+Tab from the first product link goes back to chapter 6 (not chapters 1–5).
await ev(`document.querySelector('#home-popular .store-card').focus()`);
let backFocus = null;
for (let i = 0; i < 6; i++) {
  await key('Tab', 'Tab', 9, true);
  backFocus = await ev(`(() => { const a = document.activeElement; const slide = a.closest('.home-slide'); return { id: slide?.id || null, hidden: !!(slide && slide.inert), inProducts: !!a.closest('#home-products') }; })()`);
  if (!backFocus.inProducts) break;
}
check('Shift+Tab out of product area lands in chapter 6', backFocus?.id === 'home-advisor' && !backFocus.hidden, backFocus);

// 4. Reduced motion: chapters in normal flow, products after chapter 6.
await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
await go('/', 1800);
const rm = await ev(layout);
check('reduced-motion: fallback, products after chapter 6 in normal flow', rm.mode.startsWith('fallback (reduced-motion') && rm.order.every((v, i, a) => i === 0 || v >= a[i - 1]) && rm.popularCards === 5, rm);
await send('Emulation.setEmulatedMedia', { features: [] });

// 5. JS off.
await send('Emulation.setScriptExecutionDisabled', { value: true });
await go('/', 1200);
const nojs = await ev(layout);
check('JS off: products after the six chapters, before footer', nojs.order.every((v, i, a) => i === 0 || v >= a[i - 1]) && nojs.popularCards === 5 && nojs.allCards === 12 && !nojs.overflowX, nojs);
await ev(`document.getElementById('home-products').scrollIntoView({ block: 'start' })`);
await sleep(300);
await shot('home-products-nojs-1366');
await send('Emulation.setScriptExecutionDisabled', { value: false });

// 6. Regression: /products still lists and sorts.
await go('/products?sort=priceAsc', 1200);
const prod = await ev(`({ cards: document.querySelectorAll('.store-card').length, sort: document.getElementById('catalog-sort')?.value, overflowX: document.documentElement.scrollWidth > innerWidth })`);
check('regression /products', prod.cards > 0 && prod.sort === 'priceAsc' && !prod.overflowX, prod);
// Popular cards link to real product pages.
await go('/', 1500);
const firstPopular = await ev(`document.querySelector('#home-popular .store-card').getAttribute('href')`);
await go(firstPopular, 900);
const pdp = await ev(`({ path: location.pathname, h1: document.querySelector('h1')?.textContent.trim() })`);
check('popular card opens its product page', pdp.path === firstPopular && !!pdp.h1, pdp);

results.exceptions = exceptions;
results.failures = failures;
results.finishedAt = new Date().toISOString();
fs.writeFileSync(path.join(qaDir, 'results.json'), JSON.stringify(results, null, 2));
console.log(`${failures === 0 ? 'ALL PASS' : failures + ' FAIL'}; page exceptions: ${exceptions.length}`);
ws.close();
proc.kill();
try { fs.rmSync(profile, { recursive: true, force: true }); } catch { /* ignore */ }
