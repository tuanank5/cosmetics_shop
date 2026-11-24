// P21 QA: product sections below the six-chapter carousel. Headless Chrome via DevTools protocol (no npm).
// Usage: node docs/qa/P20B/home-products.qa.mjs <chrome.exe> <baseUrl> <repoRoot>
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const [, , chrome, base, repo] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P20B');
const shotDir = path.join(repo, 'docs/ui/screenshots/P20B');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p20b-chrome-'));
const port = 9353;
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


const probe = `(() => {
  const imgs = [...document.querySelectorAll('main img, .store-card img, .pdp img, .cart img')].filter((i) => !i.closest('header, footer'));
  const broken = imgs.filter((i) => i.complete && i.naturalWidth === 0).map((i) => i.getAttribute('src'));
  const distorted = imgs.filter((i) => i.naturalWidth && i.clientWidth && getComputedStyle(i).objectFit !== 'cover' && getComputedStyle(i).objectFit !== 'contain'
    && Math.abs(i.clientWidth / i.clientHeight - i.naturalWidth / i.naturalHeight) > 0.02).map((i) => i.getAttribute('src'));
  return { w: innerWidth, overflowX: document.documentElement.scrollWidth > innerWidth, productImgs: imgs.filter((i) => i.src.includes('/img/products/')).length,
    placeholders: [...document.querySelectorAll('[data-catalog-placeholder], [class*="placeholder"]')].filter((e) => !e.hidden && !e.closest('header, footer')).length, broken, distorted };
})()`;
const pages = [['/products', 'products'], ['/', 'home'], ['/products/demo-r001', 'detail']];
for (const [w, h] of [[1366, 900], [390, 844]]) {
  await viewport(w, h);
  for (const [url, name] of pages) {
    await go(url, 1500);
    if (name === 'home') { await ev(`document.querySelectorAll('.home-carousel-pagination button')[4]?.click()`); await sleep(1300); }
    const s = await ev(probe);
    check(`${name} ${w}`, !s.overflowX && s.broken.length === 0 && s.distorted.length === 0, s);
    if (name !== 'home' || w === 1366) await shot(`${name}-${w}`);
  }
}
// Cart (guest session cart): add one SKU from the detail page, then view /cart.
await viewport(1366, 900);
await go('/products/demo-r001', 1200);
await ev(`document.querySelector('form[action*="/cart"] button[type=submit], form[action*="/cart"] button')?.click()`);
await sleep(1500);
await go('/cart', 1200);
const cart = await ev(probe);
check('cart 1366', !cart.overflowX && cart.broken.length === 0 && cart.distorted.length === 0, cart);
await shot('cart-1366');
results.exceptions = exceptions; results.failures = failures; results.finishedAt = new Date().toISOString();
fs.writeFileSync(path.join(qaDir, 'results.json'), JSON.stringify(results, null, 2));
console.log(`${failures === 0 ? 'ALL PASS' : failures + ' FAIL'}; page exceptions: ${exceptions.length}`);
ws.close(); proc.kill();
try { fs.rmSync(profile, { recursive: true, force: true }); } catch { /* ignore */ }
