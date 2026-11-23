// P19 QA: brand logo in header, home hero, footer, Admin, favicon. Headless Chrome via DevTools protocol (no npm).
// Usage: node docs/qa/P19/brand-logo.qa.mjs <chrome.exe> <baseUrl> <repoRoot>
// Admin check: registers a throwaway account, promotes it to ADMIN with the MariaDB CLI for the duration of the run,
// then demotes and deactivates it. Password is random, in memory only. DB credentials come from DB_URL/DB_USER/DB_PASSWORD.
import { spawn, spawnSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const [, , chrome, base, repo] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P19');
const shotDir = path.join(repo, 'docs/ui/screenshots/P19');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p19-chrome-'));
const port = 9343;
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
const go = async (url, wait = 1200) => { const l = loaded(); await send('Page.navigate', { url: /^(https?|data):/.test(url) ? url : base + url }); await l; await sleep(wait); };
const viewport = (w, h, dpr = 1) => send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: dpr, mobile: w < 768 });
const results = { base, startedAt: new Date().toISOString(), viewports: {}, products: {}, animation: {}, reducedMotion: {}, noJs: {}, carousel: {}, favicon: {}, footer: {}, placeholder: {}, error: {}, admin: {} };
const log = (name, pass, detail) => console.log(`${pass ? 'PASS' : 'FAIL'} ${name}${detail ? ' ' + JSON.stringify(detail) : ''}`);

// Shared probe: overflow, header logo, h1 count, hero overlap (home only).
const probe = `(() => {
  const r = (el) => { if (!el) return null; const b = el.getBoundingClientRect(); return { x: Math.round(b.x), y: Math.round(b.y), w: Math.round(b.width), h: Math.round(b.height), r: Math.round(b.right), b: Math.round(b.bottom) }; };
  const hit = (a, b) => a && b && a.w > 0 && b.w > 0 && a.x < b.r && b.x < a.r && a.y < b.b && b.y < a.b;
  const root = document.querySelector('[data-home-carousel]');
  const brand = document.querySelector('.site-header .brand');
  const img = brand?.querySelector('img');
  const summary = document.querySelector('.site-header .header-menu > summary');
  const cart = document.querySelector('.site-header .nav-shell > .header-action, .site-header .header-action');
  const search = document.querySelector('.site-header .header-search');
  const visible = (el) => el && getComputedStyle(el).display !== 'none' && el.getBoundingClientRect().width > 0;
  const hero = document.querySelector('.bs-hero');
  const heroR = r(hero), h1R = r(document.getElementById('home-title')), spotR = r(document.querySelector('#home-opening .story-spotlight'));
  const pagR = root?.classList.contains('is-ready') ? r(document.querySelector('.home-carousel-pagination')) : null;
  const brandR = r(img);
  return {
    w: innerWidth, h: innerHeight, overflowX: document.documentElement.scrollWidth > innerWidth, h1: document.querySelectorAll('h1').length,
    mode: root ? (root.classList.contains('is-ready') ? 'carousel' : 'fallback (' + (root.dataset.carouselFallback || '?') + ')') : null,
    logo: img ? { src: (img.currentSrc || '').replace(location.origin, ''), loaded: img.complete && img.naturalWidth > 0, height: brandR.h, width: brandR.w,
      opacity: getComputedStyle(brand).opacity, label: brand.getAttribute('aria-label') } : null,
    headerOverlap: [visible(summary) && hit(brandR, r(summary)) ? 'menu' : null, visible(cart) && hit(brandR, r(cart)) ? 'cart' : null,
      visible(search) && hit(brandR, r(search)) ? 'search' : null].filter(Boolean),
    hero: heroR ? { rect: heroR, overlap: [hit(heroR, h1R) ? 'h1' : null, hit(heroR, spotR) ? 'spotlight' : null, hit(heroR, pagR) ? 'pagination' : null].filter(Boolean),
      insideX: heroR.x >= 0 && heroR.r <= innerWidth } : null,
    slide1Overflow: root && root.classList.contains('is-ready') ? document.getElementById('home-opening').scrollHeight > root.clientHeight + 2 : null
  };
})()`;

// 1. Viewports on home and /products.
const vps = [[340, 720, 1, '340x720'], [360, 800, 1, '360x800'], [390, 844, 1, '390x844'], [768, 1024, 1, '768x1024'],
  [1366, 768, 1, '1366x768'], [1920, 1080, 1, '1920x1080'], [683, 384, 2, '1366x768-zoom200']];
for (const [w, h, dpr, name] of vps) {
  await viewport(w, h, dpr);
  await go('/', 1500);
  const s = await ev(probe);
  // In carousel mode the header lockup is hidden on chapter 1 (hero replaces it); check it on chapter 2.
  let s2 = null;
  if (s.mode === 'carousel') {
    await ev(`document.querySelectorAll('.home-carousel-pagination button')[1].click()`); await sleep(1200);
    s2 = await ev(probe);
  }
  const header = s2 ?? s;
  const expectMark = w < 360;
  const logoOk = header.logo?.loaded && header.logo.src.endsWith(expectMark ? 'beautyshop-mark.svg' : 'beautyshop-lockup.svg')
    && header.logo.opacity === '1' && header.logo.label === 'BeautyShop – Trang chủ';
  const r = { mode: s.mode, overflowX: s.overflowX || !!s2?.overflowX, h1: s.h1, logo: header.logo, headerOverlap: header.headerOverlap,
    chapter1BrandOpacity: s.mode === 'carousel' ? s.logo?.opacity : null, hero: s.hero, slide1Overflow: s.slide1Overflow };
  r.pass = !r.overflowX && r.h1 === 1 && logoOk && r.headerOverlap.length === 0 && r.hero && r.hero.overlap.length === 0 && r.hero.insideX
    && (w < 1366 || r.mode === 'carousel');
  results.viewports[name] = r;
  log(`home ${name} ${r.mode}`, r.pass, { logo: `${header.logo?.src} ${header.logo?.width}x${header.logo?.height}`, hero: r.hero?.rect, overlap: [...r.headerOverlap, ...(r.hero?.overlap || [])] });
  if (name === '390x844') { await go('/', 4500); await shot('hero-390x844'); }

  await go('/products', 800);
  const p = await ev(probe);
  const pOk = !p.overflowX && p.h1 === 1 && p.logo?.loaded && p.logo.src.endsWith(expectMark ? 'beautyshop-mark.svg' : 'beautyshop-lockup.svg') && p.headerOverlap.length === 0;
  results.products[name] = { overflowX: p.overflowX, h1: p.h1, logo: p.logo, headerOverlap: p.headerOverlap, pass: pOk };
  log(`products header ${name}`, pOk, { logo: `${p.logo?.src} ${p.logo?.width}x${p.logo?.height}` });
  if (name === '1366x768') await shot('header-storefront-1366', { x: 0, y: 0, width: 1366, height: 170 });
  if (name === '340x720') await shot('header-storefront-340-monogram', { x: 0, y: 0, width: 340, height: 200 });
}

// 2. Hero animation order at 1366x768: letters rise in sequence, then rules/tagline, sheen once.
await viewport(1366, 768);
{
  await go('/', 150);
  const early = await ev(`[...document.querySelectorAll('.bs-hero .bs-letter')].map((p) => Number(getComputedStyle(p).opacity))`);
  const timing = await ev(`(() => { const g = (sel) => [...document.querySelectorAll(sel)].map((el) => getComputedStyle(el).animationDelay);
    return { draw: g('.bs-hero .bs-draw'), letters: g('.bs-hero .bs-letter'), mono: g('.bs-hero .bs-mono'), rules: g('.bs-hero .bs-rule'), tagline: g('.bs-hero .bs-tagline'), sheen: g('.bs-hero .bs-sheen'),
      iterations: [...new Set([...document.querySelectorAll('.bs-hero *')].map((el) => getComputedStyle(el).animationIterationCount))] }; })()`);
  await sleep(1300); await shot('hero-1366x768-mid-animation');
  await sleep(3200);
  const late = await ev(`[...document.querySelectorAll('.bs-hero .bs-letter, .bs-hero .bs-tagline, .bs-hero .bs-mono')].map((p) => Number(getComputedStyle(p).opacity))`);
  const running = await ev(`document.getAnimations().filter((a) => a.effect?.target?.closest?.('.bs-hero') && a.playState === 'running').length`);
  await shot('hero-1366x768');
  // Go to chapter 2 and back: animations must not replay.
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[1].click()`); await sleep(1100);
  await ev(`document.querySelectorAll('.home-carousel-pagination button')[0].click()`); await sleep(300);
  const replay = await ev(`document.getAnimations().filter((a) => a.effect?.target?.closest?.('.bs-hero') && a.playState === 'running').length`);
  const secs = (list) => list.map((d) => parseFloat(d));
  const letters = secs(timing.letters);
  const ordered = letters.every((d, i) => i === 0 || d > letters[i - 1]) && Math.max(...secs(timing.draw)) < letters[0] + .5
    && Math.min(...secs(timing.rules)) > letters.at(-1) && Math.min(...secs(timing.sheen)) > letters.at(-1);
  results.animation = { earlyLetterOpacity: early, timing, finalOpacity: late, runningAfter4_5s: running, runningAfterReturnToChapter1: replay,
    pass: early.every((o) => o < 1) && ordered && late.every((o) => o === 1) && running === 0 && replay === 0 && !timing.iterations.includes('infinite') };
  log('hero animation order, once, ends visible', results.animation.pass, { letters: timing.letters.join(' '), rules: timing.rules, sheen: timing.sheen, running, replay });
}

// 3. Carousel flow 1 -> 6 -> footer -> 1 unchanged.
{
  await go('/', 1200);
  const seq = [];
  for (let i = 0; i < 7; i++) { await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: 683, y: 384, deltaX: 0, deltaY: 400 }); await sleep(1300); seq.push(await ev(`document.querySelector('[data-home-carousel]').dataset.activeSlide + '@' + Math.round(scrollY)`)); }
  for (let i = 0; i < 9; i++) { await send('Input.dispatchMouseEvent', { type: 'mouseWheel', x: 683, y: 384, deltaX: 0, deltaY: -400 }); await sleep(1300); seq.push(await ev(`document.querySelector('[data-home-carousel]').dataset.activeSlide + '@' + Math.round(scrollY)`)); }
  const slides = seq.map((x) => x.split('@')[0]);
  const ups = slides.slice(7).filter((x, i, all) => x !== all[i - 1]);
  const pass = slides.slice(0, 5).join() === '2,3,4,5,6' && seq.slice(5, 7).some((x) => Number(x.split('@')[1]) > 0) && slides.at(-1) === '1' && ups.join() === '6,5,4,3,2,1';
  results.carousel = { seq: seq.join(' '), pass };
  log('carousel wheel 1->6->footer->1', pass, { seq: results.carousel.seq });
}

// 4. Reduced motion: static hero immediately.
{
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
  await go('/', 100);
  const op = await ev(`[...document.querySelectorAll('.bs-hero .bs-letter, .bs-hero .bs-tagline, .bs-hero .bs-mono, .bs-hero .bs-star')].map((p) => Number(getComputedStyle(p).opacity))`);
  const anims = await ev(`document.getAnimations().filter((a) => a.effect?.target?.closest?.('.bs-hero')).length`);
  const sheen = await ev(`getComputedStyle(document.querySelector('.bs-hero .bs-sheen')).opacity`);
  results.reducedMotion = { opacities: [...new Set(op)], heroAnimations: anims, sheenOpacity: sheen, pass: op.every((o) => o === 1) && anims === 0 && sheen === '0' };
  log('reduced motion: static hero at once', results.reducedMotion.pass, { anims, sheen });
  await shot('hero-1366x768-reduced-motion');
  await send('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'no-preference' }] });
}

// 5. JavaScript disabled: logo still renders (CSS-only animation ends visible), header lockup visible.
{
  await send('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/', 4500);
  await send('Emulation.setScriptExecutionDisabled', { value: false });
  const s = await ev(probe);
  const op = await ev(`[...document.querySelectorAll('.bs-hero .bs-letter, .bs-hero .bs-tagline')].map((p) => Number(getComputedStyle(p).opacity))`);
  results.noJs = { heroOpacities: [...new Set(op)], logo: s.logo, overflowX: s.overflowX, heroOverlap: s.hero?.overlap,
    pass: op.every((o) => o === 1) && s.logo?.loaded && s.logo.opacity === '1' && !s.overflowX && s.hero?.overlap.length === 0 };
  log('no-JS: hero and header logo visible', results.noJs.pass, { heroOpacities: results.noJs.heroOpacities, heroOverlap: s.hero?.overlap });
}

// 6. Favicon, footer wordmark, product placeholder mark, error page.
{
  await go('/', 600);
  const fav = await ev(`(async () => { const link = document.querySelector('link[rel="icon"]'); const r = await fetch(link.href); const t = await r.text();
    return { href: link.getAttribute('href'), type: link.type, status: r.status, contentType: r.headers.get('content-type'), hasMark: t.includes('bs-mark') }; })()`);
  results.favicon = { ...fav, tabDisplay: 'chưa kiểm chứng (headless không có thanh tab)', pass: fav.status === 200 && /svg/.test(fav.contentType) && fav.hasMark && fav.type === 'image/svg+xml' };
  log('favicon served as SVG', results.favicon.pass, fav);
  const foot = await ev(`(() => { const i = document.querySelector('.footer-brand img'); const b = i.getBoundingClientRect(); return { src: i.getAttribute('src'), alt: i.alt, loaded: i.complete && i.naturalWidth > 0, h: Math.round(b.height), w: Math.round(b.width) }; })()`);
  results.footer = { ...foot, pass: foot.loaded && foot.alt === 'BeautyShop' && foot.h === 28 };
  log('footer wordmark 28px', results.footer.pass, foot);
  await go('/products', 800);
  const ph = await ev(`(() => { const imgs = [...document.querySelectorAll('.store-card-monogram img')].filter((i) => i.getBoundingClientRect().width > 0);
    const i = imgs[0]; if (!i) return { visible: 0 }; i.scrollIntoView({ block: 'center' }); const b = i.getBoundingClientRect(); const box = i.closest('.store-card-media').getBoundingClientRect();
    return { visible: imgs.length, loaded: i.complete && i.naturalWidth > 0, w: Math.round(b.width), h: Math.round(b.height), inside: b.left >= box.left && b.right <= box.right && b.top >= box.top && b.bottom <= box.bottom }; })()`);
  results.placeholder = { ...ph, pass: ph.visible > 0 && ph.loaded && ph.inside && ph.w === 48 };
  log('placeholder monogram image', results.placeholder.pass, ph);
  await go('/san-pham-khong-ton-tai-p19', 600);
  const er = await ev(probe);
  results.error = { h1: er.h1, logo: er.logo, overflowX: er.overflowX, pass: er.h1 === 1 && er.logo?.loaded && !er.overflowX };
  log('404 page header lockup', results.error.pass, { logo: er.logo?.src });
}

// 7. Admin header (light lockup) with a temporary admin account.
{
  const m = /jdbc:mariadb:\/\/([^:/]+)(?::(\d+))?\/([^?]+)/.exec(process.env.DB_URL || '');
  const cli = 'C:/Program Files/MariaDB 12.2/bin/mariadb.exe';
  const sql = (q) => spawnSync(cli, ['-h', m[1], '-P', m[2] || '3306', '-u', process.env.DB_USER, m[3], '-e', q], { env: { ...process.env, MYSQL_PWD: process.env.DB_PASSWORD }, encoding: 'utf8' });
  if (!m || !process.env.DB_USER || !fs.existsSync(cli)) {
    results.admin = { verified: false, note: 'Thiếu DB env hoặc MariaDB CLI: chưa kiểm chứng trên trình duyệt.' };
    console.log('SKIP admin header');
  } else {
    const email = `qa-p19-${crypto.randomUUID().slice(0, 8)}@example.test`;
    const password = crypto.randomBytes(18).toString('base64url');
    await send('Network.clearBrowserCookies');
    await go('/register', 500);
    { const l = loaded();
      await ev(`(() => { const f = document.querySelector('form[action="/register"]'); f.hoTen.value = 'QA P19'; f.email.value = ${JSON.stringify(email)};
        f.matKhau.value = ${JSON.stringify(password)}; const c = f.querySelector('[name="xacNhanMatKhau"]'); if (c) c.value = ${JSON.stringify(password)};
        f.querySelectorAll('input[type=checkbox][required]').forEach((x) => { x.checked = true; }); f.requestSubmit(); return true; })()`);
      await l; await sleep(300); }
    const promoted = sql(`UPDATE nguoi_dung SET vai_tro='ADMIN' WHERE email='${email}'`);
    await send('Network.clearBrowserCookies');
    await go('/login', 400);
    { const l = loaded();
      await ev(`(() => { const f = document.querySelector('form[action="/login"]'); f.username.value = ${JSON.stringify(email)}; f.password.value = ${JSON.stringify(password)}; f.requestSubmit(); return true; })()`);
      await l; await sleep(400); }
    const out = {};
    for (const [w, h, name] of [[1366, 768, '1366'], [768, 1024, '768'], [390, 844, '390'], [340, 720, '340']]) {
      await viewport(w, h);
      await go('/admin/catalog', 700);
      const s = await ev(probe);
      out[name] = { url: await ev('location.pathname'), logo: s.logo, overflowX: s.overflowX, h1: s.h1, headerOverlap: s.headerOverlap,
        pass: s.logo?.loaded && s.logo.src.endsWith(w < 360 ? 'beautyshop-mark.svg' : 'beautyshop-lockup-light.svg') && !s.overflowX && s.h1 === 1 && s.headerOverlap.length === 0 };
      log(`admin header ${name}`, out[name].pass, { url: out[name].url, logo: `${s.logo?.src} ${s.logo?.width}x${s.logo?.height}` });
      if (name === '1366') await shot('header-admin-1366', { x: 0, y: 0, width: 1366, height: 170 });
    }
    const cleanup = sql(`UPDATE nguoi_dung SET vai_tro='CUSTOMER', kich_hoat=FALSE WHERE email='${email}'`);
    results.admin = { verified: true, account: 'qa-p19-<random>@example.test', promoteStatus: promoted.status, cleanupStatus: cleanup.status,
      cleanup: 'vai_tro=CUSTOMER, kich_hoat=FALSE', viewports: out, pass: promoted.status === 0 && cleanup.status === 0 && Object.values(out).every((x) => x.pass) };
  }
}

// 8. Favicon close-up (rendered from the served file).
// QA-only: swap the DOM of a same-origin page (in the QA browser only) to show the served favicon at 128/32/16px.
await viewport(320, 160, 1);
await go('/login', 400);
await ev(`(() => { document.head.innerHTML = ''; document.body.innerHTML = ['128', '32', '16'].map((n) => '<img src="/img/favicon.svg" width="' + n + '" height="' + n + '" alt="">').join(' ');
  document.body.setAttribute('bgcolor', '#2b2b2e'); return Promise.all([...document.images].map((i) => i.decode())).then(() => [...document.images].every((i) => i.naturalWidth > 0)); })()`);
await shot('favicon-128');

results.exceptions = exceptions;
results.finishedAt = new Date().toISOString();
const checks = [...Object.values(results.viewports), ...Object.values(results.products), results.animation, results.carousel, results.reducedMotion,
  results.noJs, results.favicon, results.footer, results.placeholder, results.error, ...(results.admin.verified ? [results.admin] : [])];
results.summary = { pass: checks.filter((c) => c.pass).length, fail: checks.filter((c) => !c.pass).length, jsExceptions: exceptions.length, adminVerified: !!results.admin.verified };
fs.writeFileSync(path.join(qaDir, 'results.json'), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results.summary));
ws.close();
proc.kill();
