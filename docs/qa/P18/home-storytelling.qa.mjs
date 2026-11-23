// P18 QA: six-chapter home carousel in headless Chrome via the DevTools protocol (no npm dependency).
// Usage: node docs/qa/P18/home-storytelling.qa.mjs <chrome.exe> <baseUrl> <repoRoot>
// Writes docs/qa/P18/results.json and selected screenshots to docs/ui/screenshots/P18/.
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const [, , chrome, base, repo] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P18');
const shotDir = path.join(repo, 'docs/ui/screenshots/P18');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p18-chrome-'));
const port = 9342;
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
const shot = async (name) => {
  const r = await send('Page.captureScreenshot', { format: 'png' });
  fs.writeFileSync(path.join(shotDir, `${name}.png`), Buffer.from(r.data, 'base64'));
};
await send('Page.enable'); await send('Runtime.enable');
const loaded = () => new Promise((r) => { const l = { method: 'Page.loadEventFired', fn: () => { listeners.splice(listeners.indexOf(l), 1); r(); } }; listeners.push(l); });
const go = async (url, wait = 1200) => { try { await ev(`scrollTo(0, 0)`); } catch { /* about:blank */ } const l = loaded(); await send('Page.navigate', { url: base + url }); await l; await sleep(wait); };
const viewport = (w, h, dpr = 1) => send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: dpr, mobile: w < 768 });
const key = async (k, code, vk) => {
  await send('Input.dispatchKeyEvent', { type: 'rawKeyDown', key: k, code, windowsVirtualKeyCode: vk });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: k, code, windowsVirtualKeyCode: vk });
};
const state = `(() => {
  const root = document.querySelector('[data-home-carousel]');
  const slides = [...document.querySelectorAll('.home-slide')];
  return { w: innerWidth, h: innerHeight, ready: root.classList.contains('is-ready'), fallback: root.dataset.carouselFallback || null,
    slide: root.dataset.activeSlide ? Number(root.dataset.activeSlide) : null, scrollY: Math.round(scrollY),
    overflowX: document.documentElement.scrollWidth > innerWidth, h1: document.querySelectorAll('h1').length,
    slides: slides.length, ids: slides.map((s) => s.id),
    overflowing: slides.map((s) => ({ id: s.id, scrollHeight: s.scrollHeight, clientHeight: root.clientHeight })).filter((s) => s.scrollHeight > s.clientHeight + 2).map((s) => s.id),
    inertOk: !root.classList.contains('is-ready') || slides.every((s, i) => (i + 1 === Number(root.dataset.activeSlide)) === !s.inert && s.inert === (s.getAttribute('aria-hidden') === 'true')),
    tallSlides: root.classList.contains('is-ready') ? [] : slides.filter((s) => s.offsetHeight > innerHeight + 2).map((s) => s.id + ':' + s.offsetHeight),
    brandSize: getComputedStyle(document.querySelector('.site-header .brand')).fontSize };
})()`;
const results = { base, startedAt: new Date().toISOString(), viewports: {}, flows: {}, links: {}, a11y: {}, noJs: {}, reducedMotion: {}, reveal: {}, regression: {}, touch: {} };
const log = (name, pass, detail) => console.log(`${pass ? 'PASS' : 'FAIL'} ${name}${detail ? ' ' + JSON.stringify(detail) : ''}`);

async function wheelFlow(w, h) {
  const seq = [];
  const start = (await ev(state)).slide;
  if (start !== 1) { await ev(`document.querySelectorAll('.home-carousel-pagination button')[0].click()`); await sleep(1300); }
  const wheel = async (dy) => {
    await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: Math.floor(w / 2), y: Math.floor(h / 2), deltaX: 0, deltaY: dy });
    await sleep(1300);
    const s = await ev(state);
    seq.push(`${s.slide ?? '-'}@${s.scrollY}`);
  };
  for (let i = 0; i < 7; i++) await wheel(400);
  for (let i = 0; i < 9; i++) await wheel(-400);
  const slidesSeen = seq.map((x) => x.split('@')[0]);
  const down = slidesSeen.slice(0, 5).join(',');
  const footerReached = seq.slice(5, 7).some((x) => x.startsWith('6@') && Number(x.split('@')[1]) > 0);
  const ups = slidesSeen.slice(7).filter((x, i, all) => x !== all[i - 1]);
  const back = slidesSeen.at(-1) === '1' && ups.join() === '6,5,4,3,2,1';
  return { start, seq: seq.join(' '), pass: down === '2,3,4,5,6' && footerReached && back };
}

// 1. Viewports: carousel enabled or fallback, overflow, wheel flow where enabled.
const vps = [[360, 800, 1, '360x800'], [390, 844, 1, '390x844'], [768, 1024, 1, '768x1024'], [1366, 768, 1, '1366x768'], [1920, 1080, 1, '1920x1080'], [683, 384, 2, '1366x768-zoom200']];
for (const [w, h, dpr, name] of vps) {
  await viewport(w, h, dpr);
  await go('/');
  const s = await ev(state);
  const r = { mode: s.ready ? 'carousel' : `fallback (${s.fallback})`, overflowX: s.overflowX, h1: s.h1, slides: s.slides, overflowingSlides: s.overflowing, tallSlidesInFallback: s.tallSlides, inertOk: s.inertOk };
  if (s.ready) r.wheel = await wheelFlow(w, h);
  await go('/');
  r.overflowXAfter = (await ev(state)).overflowX;
  r.pass = !r.overflowX && !r.overflowXAfter && r.h1 === 1 && r.slides === 6 && r.inertOk && (!r.wheel || r.wheel.pass);
  results.viewports[name] = r;
  log(`viewport ${name} ${r.mode}`, r.pass, r.wheel ? { wheel: r.wheel.seq } : { tallSlides: r.tallSlidesInFallback });
  if (name === '390x844') {
    await shot('mobile-390-chapter1');
    await ev(`document.getElementById('home-ritual').scrollIntoView({ block: 'start' })`); await sleep(400);
    await shot('mobile-390-chapter3-fallback');
  }
}

// 2. Desktop 1366: keyboard, pagination, brand size, reveal, screenshots of every chapter.
await viewport(1366, 768);
await go('/');
{
  const seq = [];
  for (let i = 0; i < 5; i++) { await key(i % 2 ? 'PageDown' : 'ArrowDown', i % 2 ? 'PageDown' : 'ArrowDown', i % 2 ? 34 : 40); await sleep(1100); seq.push((await ev(state)).slide); }
  const up = [];
  for (let i = 0; i < 5; i++) { await key(i % 2 ? 'PageUp' : 'ArrowUp', i % 2 ? 'PageUp' : 'ArrowUp', i % 2 ? 33 : 38); await sleep(1100); up.push((await ev(state)).slide); }
  results.flows.keyboard = { down: seq, up, pass: seq.join() === '2,3,4,5,6' && up.join() === '5,4,3,2,1' };
  log('keyboard arrows/page keys', results.flows.keyboard.pass, results.flows.keyboard);
}
{
  const bullets = await ev(`document.querySelectorAll('.home-carousel-pagination button').length`);
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[3].click()`); await sleep(1200);
  const at4 = (await ev(state)).slide;
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[0].click()`); await sleep(1200);
  const at1 = (await ev(state)).slide;
  const labels = await ev(`[...document.querySelectorAll('.home-slide')].map((s) => s.getAttribute('aria-label'))`);
  results.flows.pagination = { bullets, at4, at1, labels, pass: bullets === 6 && at4 === 4 && at1 === 1 && labels[5] === 'Chương 6 / 6' };
  log('pagination 6 bullets', results.flows.pagination.pass, { bullets, at4, at1, label6: labels[5] });
}
{
  // Anchor link inside chapter 1 jumps to chapter 2 (handled by the carousel click handler).
  await ev(`document.querySelector('#home-opening .story-next').click()`); await sleep(1200);
  const s = await ev(state);
  results.flows.anchor = { slide: s.slide, pass: s.slide === 2 };
  log('anchor #home-skin from chapter 1', s.slide === 2);
}
{
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[0].click()`); await sleep(1200);
  const brand1 = (await ev(state)).brandSize;
  // Before chapter 3 is shown its items are hidden (stagger pending); after activation they reveal.
  const hiddenBefore = await ev(`[...document.querySelectorAll('#home-ritual .story-reveal-list > li')].every((li) => getComputedStyle(li).opacity === '0')`);
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[2].click()`); await sleep(1800);
  const shownAfter = await ev(`[...document.querySelectorAll('#home-ritual .story-reveal-list > li')].map((li) => getComputedStyle(li).opacity)`);
  const delays = await ev(`[...document.querySelectorAll('#home-ritual .story-reveal-list > li')].map((li) => getComputedStyle(li).transitionDelay)`);
  const brand3 = (await ev(state)).brandSize;
  results.reveal = { hiddenBefore, shownAfter, delays, brand1, brand3, pass: hiddenBefore && shownAfter.every((o) => o === '1') && new Set(delays).size > 1 && parseFloat(brand1) > 60 && brand3 === '24px' };
  log('staggered reveal + small brand after chapter 1', results.reveal.pass, { delays, brand1, brand3 });
}
for (let i = 0; i < 6; i++) {
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[${i}].click()`); await sleep(1700);
  await shot(`desktop-1366-chapter${i + 1}`);
}

// 3. Keyboard focus: Tab never lands inside an inactive slide; focus ring is visible.
{
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[1].click()`); await sleep(1300);
  await ev(`document.activeElement?.blur(); document.body.focus()`);
  const visits = [];
  for (let i = 0; i < 40; i++) {
    await key('Tab', 'Tab', 9); await sleep(60);
    visits.push(await ev(`(() => { const a = document.activeElement; const slide = a?.closest('.home-slide');
      const cs = a ? getComputedStyle(a) : null;
      return { tag: a?.tagName, text: (a?.textContent || '').trim().slice(0, 30), slide: slide ? [...document.querySelectorAll('.home-slide')].indexOf(slide) + 1 : null,
        active: Number(document.querySelector('[data-home-carousel]').dataset.activeSlide), outline: cs ? cs.outlineStyle !== 'none' && cs.outlineWidth !== '0px' : false }; })()`));
  }
  const leaked = visits.filter((v) => v.slide !== null && v.slide !== v.active);
  const inSlide = visits.filter((v) => v.slide !== null);
  results.a11y.tab = { leaked, focusedInActive: inSlide.length, outlinesVisible: inSlide.every((v) => v.outline), pass: leaked.length === 0 && inSlide.length > 0 && inSlide.every((v) => v.outline) };
  log('Tab stays in active chapter, focus visible', results.a11y.tab.pass, { leaked: leaked.length, focusedInActive: inSlide.length });
}

// 4. Every chapter link resolves with results.
{
  await go('/');
  const links = await ev(`(() => ({
    skin: [...document.querySelectorAll('#home-skin a[href^="/products"]')].map((a) => a.getAttribute('href')),
    ritual: [...document.querySelectorAll('#home-ritual a[href^="/products"]')].map((a) => a.getAttribute('href')),
    ingredients: [...document.querySelectorAll('#home-ingredients a[href^="/products"]')].map((a) => a.getAttribute('href')),
    featured: [...document.querySelectorAll('#home-featured a.store-card')].map((a) => a.getAttribute('href')),
    advisor: [...document.querySelectorAll('#home-advisor a[href="/chat"]')].map((a) => a.getAttribute('href')) }))()`);
  const counts = {};
  for (const group of ['skin', 'ritual', 'ingredients']) {
    for (const href of links[group]) {
      counts[href] = await ev(`fetch(${JSON.stringify(href)}).then((r) => r.text()).then((t) => new DOMParser().parseFromString(t, 'text/html').querySelectorAll('.store-grid > li').length)`);
    }
  }
  const pdp = {};
  for (const href of links.featured) pdp[href] = await ev(`fetch(${JSON.stringify(href)}).then(async (r) => ({ status: r.status, h1: new DOMParser().parseFromString(await r.text(), 'text/html').querySelector('h1')?.textContent.trim() }))`);
  const chat = await ev(`fetch('/chat').then((r) => r.status)`);
  results.links = { counts, pdp, chat, sizes: { skin: links.skin.length, ritual: links.ritual.length, ingredients: links.ingredients.length, featured: links.featured.length },
    pass: Object.values(counts).every((n) => n > 0) && links.skin.length > 0 && links.ritual.length === 5 && links.ingredients.length > 0
      && links.featured.length === 4 && Object.values(pdp).every((p) => p.status === 200 && p.h1) && chat === 200 };
  log('chapter links return results / PDP / chat', results.links.pass, results.links.sizes);
}

// 5. JavaScript disabled: six chapters visible, all items visible, links present.
{
  await viewport(1366, 768);
  await send('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/', 400);
  await send('Emulation.setScriptExecutionDisabled', { value: false });
  const r = await ev(`(() => { const slides = [...document.querySelectorAll('.home-slide')];
    const items = [...document.querySelectorAll('.story-reveal-list > li')];
    return { slides: slides.length, visible: slides.every((s) => s.offsetHeight > 0 && !s.inert && s.getAttribute('aria-hidden') !== 'true'),
      itemsVisible: items.every((li) => getComputedStyle(li).opacity === '1'), items: items.length,
      links: document.querySelectorAll('.home-slide a[href]').length, overflowX: document.documentElement.scrollWidth > innerWidth }; })()`);
  results.noJs = { ...r, pass: r.slides === 6 && r.visible && r.itemsVisible && r.items >= 13 && !r.overflowX };
  log('no-JS: 6 chapters and all items visible', results.noJs.pass, r);
}

// 6. Reduced motion: fallback, items visible.
{
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
  await go('/');
  const s = await ev(state);
  const items = await ev(`[...document.querySelectorAll('.story-reveal-list > li')].every((li) => getComputedStyle(li).opacity === '1')`);
  results.reducedMotion = { fallback: s.fallback, itemsVisible: items, slides: s.slides, pass: s.fallback === 'reduced-motion' && items && s.slides === 6 };
  log('reduced motion: fallback, items visible', results.reducedMotion.pass, { fallback: s.fallback });
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'no-preference' }] });
}

// 7. Touch swipe on the first viewport that runs the carousel below desktop width (if any).
{
  const touchVp = Object.entries(results.viewports).find(([n, r]) => r.mode === 'carousel' && /^(360|390|768)/.test(n));
  if (!touchVp) {
    results.touch = { verified: false, note: 'No narrow viewport runs the carousel (all fall back to native scrolling); touch swipe not exercised.' };
    console.log('SKIP touch swipe: no narrow viewport in carousel mode');
  } else {
    const [w, h] = touchVp[0].split('x').map(Number);
    await viewport(w, h);
    await send('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 5 });
    await go('/');
    await send('Input.synthesizeScrollGesture', { x: Math.floor(w / 2), y: Math.floor(h * .7), yDistance: -Math.floor(h * .5), gestureSourceType: 'touch', speed: 1200 });
    await sleep(1300);
    const s = await ev(state);
    results.touch = { viewport: touchVp[0], slideAfterSwipe: s.slide, verified: true, pass: s.slide === 2 };
    log(`touch swipe ${touchVp[0]}`, results.touch.pass, { slide: s.slide });
    await send('Emulation.setTouchEmulationEnabled', { enabled: false });
  }
}

// 8. Regression: /products filters, sort, paging, legacy search.
{
  await viewport(1366, 900);
  const probe = async (url) => ev(`fetch(${JSON.stringify(url)}).then(async (r) => { const d = new DOMParser().parseFromString(await r.text(), 'text/html');
    return { status: r.status, items: d.querySelectorAll('.store-grid > li').length, count: d.querySelector('.catalog-count')?.textContent.replace(/\\s+/g, ' ').trim() ?? null }; })`);
  const urls = ['/products', '/products?sort=priceAsc', '/products?sort=priceDesc', '/products?page=1&size=12', '/products?q=cerave',
    '/products?q=DEMO-001', '/products?category=demo-duong-am', '/products?skinType=Da%20kh%C3%B4', '/products?q=hyaluronic', '/products?q=%25'];
  for (const u of urls) results.regression[u] = await probe(u);
  const r = results.regression;
  results.regression.pass = Object.values(r).every((x) => x.status === 200) && r['/products'].items > 0 && r['/products?page=1&size=12'].items > 0
    && r['/products?q=cerave'].items > 0 && r['/products?q=DEMO-001'].items > 0 && r['/products?q=hyaluronic'].items > 0;
  log('products regression', results.regression.pass);
}

results.exceptions = exceptions;
results.finishedAt = new Date().toISOString();
const checks = [...Object.values(results.viewports), ...Object.values(results.flows), results.links, results.a11y.tab, results.noJs,
  results.reducedMotion, results.reveal, results.regression, ...(results.touch.verified ? [results.touch] : [])];
results.summary = { pass: checks.filter((c) => c.pass).length, fail: checks.filter((c) => !c.pass).length, jsExceptions: exceptions.length };
fs.writeFileSync(path.join(qaDir, 'results.json'), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results.summary));
ws.close();
proc.kill();
