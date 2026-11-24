// P23 QA: đánh giá sao + bình luận lưu ở MongoDB. Chrome qua DevTools protocol (không npm).
// Usage:
//   node docs/qa/P23/danh-gia.qa.mjs mongo-off <chrome.exe> <baseUrl> <repoRoot> [pdpPath]
//       headless, Guest; chạy khi service MongoDB đang dừng: các trang chính vẫn 200, chi tiết báo "Tạm thời...".
//   node docs/qa/P23/danh-gia.qa.mjs form-css <chrome.exe> <baseUrl> <repoRoot>
//       headless; kiểm lại CSS form đánh giá (markup như server render, CSS/JS thật của app).
//   node docs/qa/P23/danh-gia.qa.mjs flow <chrome.exe> <baseUrl> <repoRoot>
//       mở cửa sổ Chrome THẤY ĐƯỢC tại /login. Owner tự đăng nhập Admin; script không tạo/đọc/nâng quyền Admin.
//       Khách hàng: đăng ký qua /register (mật khẩu ngẫu nhiên, chỉ trong bộ nhớ) trong một browser context riêng,
//       đặt 2 đơn COD qua UI; Admin chuyển đơn 1 tới DELIVERED; khách đánh giá -> Admin duyệt -> sửa -> duyệt lại.
import { spawn } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const [, , mode, chrome, base, repo, pdpArg] = process.argv;
const qaDir = path.join(repo, 'docs/qa/P23');
const shotDir = path.join(repo, 'docs/ui/screenshots/P23');
fs.mkdirSync(shotDir, { recursive: true });
const profile = fs.mkdtempSync(path.join(os.tmpdir(), 'p23-chrome-'));
const port = mode === 'flow' ? 9356 : 9355;
const flags = [`--remote-debugging-port=${port}`, `--user-data-dir=${profile}`, '--no-first-run', '--hide-scrollbars'];
if (mode !== 'flow') flags.unshift('--headless=new');
const proc = spawn(chrome, [...flags, mode === 'flow' ? `${base}/login` : 'about:blank'], { stdio: 'ignore' });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
for (let i = 0; i < 60; i++) { try { await fetch(`http://127.0.0.1:${port}/json/version`); break; } catch { await sleep(200); } }

const results = { mode, base, startedAt: new Date().toISOString(), checks: [], notes: [] };
let failures = 0;
const check = (name, pass, detail) => {
  results.checks.push({ name, pass, detail });
  if (!pass) failures++;
  console.log(`${pass ? 'PASS' : 'FAIL'} ${name}${detail !== undefined ? ' ' + JSON.stringify(detail) : ''}`);
};
const note = (k, v) => { results.notes.push({ [k]: v }); console.log(`NOTE ${k} ${JSON.stringify(v)}`); };

/** Một tab (page target) điều khiển qua WebSocket riêng. */
async function openPage(wsUrl, label) {
  const ws = new WebSocket(wsUrl);
  await new Promise((r) => { ws.onopen = r; });
  let nextId = 0;
  const pending = new Map();
  const listeners = [];
  const exceptions = [];
  let lastDocStatus = null;
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); return; }
    if (m.method === 'Runtime.exceptionThrown') exceptions.push(m.params.exceptionDetails?.exception?.description || m.params.exceptionDetails?.text);
    if (m.method === 'Network.responseReceived' && m.params.type === 'Document') lastDocStatus = m.params.response.status;
    if (m.method) for (const l of [...listeners]) if (l.method === m.method) l.fn(m.params);
  };
  const send = (method, params = {}) => new Promise((r) => { const id = ++nextId; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })); })
    .then((m) => { if (m.error) throw new Error(`${label} ${method}: ${m.error.message}`); return m.result; });
  const ev = async (expression) => {
    const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (r.exceptionDetails) throw new Error(`${label}: ` + (r.exceptionDetails.exception?.description || r.exceptionDetails.text));
    return r.result.value;
  };
  await send('Page.enable'); await send('Runtime.enable'); await send('Network.enable');
  const loaded = () => new Promise((r) => { const l = { method: 'Page.loadEventFired', fn: () => { listeners.splice(listeners.indexOf(l), 1); r(); } }; listeners.push(l); });
  const go = async (url, wait = 700) => {
    await send('Page.navigate', { url: 'about:blank' }); await sleep(100);
    lastDocStatus = null;
    const l = loaded(); await send('Page.navigate', { url: /^https?:/.test(url) ? url : base + url });
    await Promise.race([l, sleep(15000)]); await sleep(wait);
    return lastDocStatus;
  };
  /** Chạy biểu thức gửi form rồi chờ trang mới tải xong. */
  const submit = async (expression, wait = 700) => {
    lastDocStatus = null;
    const l = loaded(); const ok = await ev(expression);
    if (ok === false) return false;
    await Promise.race([l, sleep(15000)]); await sleep(wait);
    return lastDocStatus;
  };
  const viewport = (w, h) => send('Emulation.setDeviceMetricsOverride', { width: w, height: h, deviceScaleFactor: 1, mobile: w < 768 });
  const shot = async (name) => {
    await send('Page.bringToFront'); await sleep(250);
    const r = await send('Page.captureScreenshot', { format: 'png' });
    fs.writeFileSync(path.join(shotDir, `${name}.png`), Buffer.from(r.data, 'base64'));
  };
  const key = async (k, code, keyCode) => {
    await send('Input.dispatchKeyEvent', { type: 'keyDown', key: k, code, windowsVirtualKeyCode: keyCode });
    await send('Input.dispatchKeyEvent', { type: 'keyUp', key: k, code, windowsVirtualKeyCode: keyCode });
  };
  const overflow = () => ev('document.documentElement.scrollWidth > innerWidth');
  return { send, ev, go, submit, viewport, shot, key, overflow, exceptions, close: () => ws.close() };
}

const listPages = async () => (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).filter((t) => t.type === 'page');
const first = await openPage((await listPages())[0].webSocketDebuggerUrl, 'tab1');

// Trạng thái khu đánh giá trên trang chi tiết (đọc DOM, không đoán).
const pdpProbe = (token) => `(() => {
  const s = document.getElementById('danh-gia');
  const items = [...document.querySelectorAll('.pdp-reviews li')];
  const tok = ${JSON.stringify(token)};
  const mine = tok ? items.find((li) => li.textContent.includes(tok)) : null;
  return {
    h1: document.querySelector('h1')?.textContent.trim(),
    tamThoi: !!s && s.textContent.includes('Tạm thời chưa tải được đánh giá'),
    chuaCo: !!s && s.textContent.includes('Chưa có đánh giá được duyệt.'),
    diem: document.querySelector('.review-score > strong')?.textContent.trim() || null,
    soDanhGia: Number((document.querySelector('.review-score .muted')?.textContent.match(/\\d+/) || [0])[0]),
    starsLabel: document.querySelector('.review-score .rating-stars')?.getAttribute('aria-label') || null,
    bars: [...document.querySelectorAll('.review-bars li')].map((li) => li.querySelector('.review-bar-count')?.textContent.trim()),
    items: items.length,
    mine: mine ? { text: mine.querySelector('.pdp-copy')?.textContent.trim(), badge: !!mine.querySelector('.review-badge'),
      stars: mine.querySelector('.rating-stars')?.getAttribute('aria-label'), name: mine.querySelector('.review-meta strong')?.textContent.trim(),
      date: mine.querySelector('time')?.textContent.trim() } : null,
    cartForm: !!document.querySelector('form[action="/cart/items"]'),
    overflowX: document.documentElement.scrollWidth > innerWidth
  };
})()`;

try {
  if (mode === 'mongo-off') {
    const pdp = pdpArg || '/products/demo-r001';
    // 1. Thời gian phản hồi phía server (Guest, không cookie): lần đầu chịu timeout chọn server 2 s, sau đó store nghỉ 15 s.
    const timing = [];
    for (const p of ['/', '/products', pdp, '/cart', pdp, '/']) {
      const t0 = performance.now();
      const r = await fetch(base + p, { redirect: 'manual' });
      const body = await r.text();
      timing.push({ path: p, status: r.status, ms: Math.round(performance.now() - t0), tamThoi: body.includes('Tạm thời chưa tải được đánh giá') });
    }
    note('server timing (Mongo stopped)', timing);
    check('all main pages 200 while MongoDB is stopped', timing.every((t) => t.status === 200), timing.map((t) => `${t.path}:${t.status}`));
    check('product page shows temporary review notice', timing.filter((t) => t.path === pdp).every((t) => t.tamThoi));
    for (const [w, h] of [[1366, 768], [390, 844]]) {
      await first.viewport(w, h);
      for (const p of ['/', '/products', '/cart']) {
        const status = await first.go(p, p === '/' ? 1500 : 700);
        const ov = await first.overflow();
        check(`${p} ${w}px 200, no horizontal scroll (Mongo stopped)`, status === 200 && !ov, { status, overflowX: ov });
      }
      const status = await first.go(pdp, 900);
      const probe = await first.ev(pdpProbe(null));
      check(`pdp ${w}px: 200, notice, cart form, no overflow (Mongo stopped)`, status === 200 && probe.tamThoi && probe.cartForm && !probe.overflowX, { status, ...probe });
      await first.ev(`document.getElementById('danh-gia').scrollIntoView({ block: 'center' })`);
      await first.shot(`p23-mongo-off-pdp-${w}`);
    }
    results.exceptions = first.exceptions;
  } else if (mode === 'form-css') {
    // Kiểm lại CSS form đánh giá sau khi sửa (sao xếp dọc ở lần chạy flow đầu): CSS + JS thật do app phục vụ,
    // markup đúng như server render order/review.html (lấy từ render DanhGiaPagesTest), gắn vào một trang storefront thật.
    const markup = `<a class="text-link" href="/orders/0"><span aria-hidden="true">←</span> Quay lại đơn</a>
      <h1>Đánh giá sản phẩm</h1><p class="review-product">Sản phẩm kiểm thử · 30 ml</p>
      <form class="catalog-form review-form" method="post" action="/orders/items/0/review" novalidate>
        <fieldset class="star-picker"><legend>Số sao <span class="muted">(bắt buộc)</span></legend>
        ${[1, 2, 3, 4, 5].map((n) => `<span class="star-option"><input type="radio" value="${n}" required id="soSao${n}" name="soSao"><label for="soSao${n}"><span aria-hidden="true">★</span><span class="visually-hidden">${n} sao</span></label></span>`).join('')}
        </fieldset>
        <label for="noiDung">Nhận xét <span class="muted">(không bắt buộc)</span></label>
        <textarea id="noiDung" maxlength="1000" rows="6" data-char-source aria-describedby="noiDung-count" name="noiDung"></textarea>
        <p id="noiDung-count" class="hint" data-char-count aria-live="polite"><span data-char-current>0</span>/<span>1000</span> ký tự</p>
        <button type="submit" class="btn btn--primary">Gửi đánh giá</button>
      </form>`;
    for (const [w, h] of [[1366, 768], [390, 844]]) {
      await first.viewport(w, h);
      await first.go('/products', 600);
      await first.ev(`(() => { const m = document.querySelector('main'); m.className = 'page-content storefront review-page'; m.innerHTML = ${JSON.stringify(markup)};
        return new Promise((r) => { const s = document.createElement('script'); s.src = '/js/review-form.js'; s.onload = r; document.body.append(s); }); })()`);
      await first.send('Page.bringToFront');
      await first.ev(`document.getElementById('soSao1').focus()`);
      for (let i = 0; i < 3; i++) await first.key('ArrowRight', 'ArrowRight', 39);
      await first.ev(`document.getElementById('noiDung').focus()`);
      await first.send('Input.insertText', { text: 'Thấm nhanh, mùi dễ chịu.' });
      await sleep(900);
      const lay = await first.ev(`(() => { const ls = [...document.querySelectorAll('.star-option label')].map((l) => l.getBoundingClientRect());
        const fs = getComputedStyle(document.querySelector('.star-picker'));
        return { display: fs.display, border: fs.borderTopWidth, tops: [...new Set(ls.map((r) => Math.round(r.top)))], minW: Math.min(...ls.map((r) => Math.round(r.width))),
          minH: Math.min(...ls.map((r) => Math.round(r.height))), checked: document.querySelector('input[name=soSao]:checked')?.value,
          gold: [...document.querySelectorAll('.star-option label')].map((l) => getComputedStyle(l).color),
          counter: document.querySelector('[data-char-current]').textContent, overflowX: document.documentElement.scrollWidth > innerWidth }; })()`);
      check(`review form CSS ${w}px: 5 stars on one row, 44px targets, 4 selected via keyboard, counter, no overflow`,
        lay.display === 'flex' && lay.tops.length === 1 && lay.minW >= 44 && lay.minH >= 44 && lay.checked === '4'
          && lay.gold[3] === lay.gold[0] && lay.gold[4] !== lay.gold[0] && lay.counter === '24' && !lay.overflowX, lay);
      await first.ev(`scrollTo(0, 0)`);
      await first.shot(`p23-review-form-${w}`);
    }
    results.exceptions = first.exceptions;
  } else if (mode === 'flow') {
    const admin = first;
    console.log('Waiting for the owner to sign in as Admin in the opened Chrome window (up to 10 minutes)...');
    let path0 = '';
    for (let i = 0; i < 600; i++) {
      try { path0 = await admin.ev('location.pathname'); } catch { path0 = ''; }
      if (path0.startsWith('/admin')) break;
      await sleep(1000);
    }
    check('owner signed in as Admin', path0.startsWith('/admin'), { path: path0 });
    if (!path0.startsWith('/admin')) throw new Error('Admin login not completed');
    await admin.viewport(1366, 768);

    // Khách hàng trong browser context riêng (cookie tách biệt với Admin).
    const browserWs = (await (await fetch(`http://127.0.0.1:${port}/json/version`)).json()).webSocketDebuggerUrl;
    let customer;
    {
      const bws = new WebSocket(browserWs); await new Promise((r) => { bws.onopen = r; });
      let id = 0; const pend = new Map();
      bws.onmessage = (e) => { const m = JSON.parse(e.data); if (m.id && pend.has(m.id)) { pend.get(m.id)(m); pend.delete(m.id); } };
      const bsend = (method, params = {}) => new Promise((r) => { const i = ++id; pend.set(i, r); bws.send(JSON.stringify({ id: i, method, params })); })
        .then((m) => { if (m.error) throw new Error(`${method}: ${m.error.message}`); return m.result; });
      const { browserContextId } = await bsend('Target.createBrowserContext', { disposeOnDetach: false });
      const { targetId } = await bsend('Target.createTarget', { url: 'about:blank', browserContextId, newWindow: true });
      await sleep(500);
      const t = (await listPages()).find((p) => p.id === targetId);
      customer = await openPage(t.webSocketDebuggerUrl, 'customer');
    }
    await customer.viewport(1366, 768);

    const email = `qa-p23-${crypto.randomUUID().slice(0, 8)}@example.test`;
    const password = crypto.randomBytes(18).toString('base64url');
    await customer.go('/register', 500);
    await customer.submit(`(() => { const f = document.querySelector('form[action="/register"]'); f.hoTen.value = 'Khách QA P23'; f.email.value = ${JSON.stringify(email)};
      f.matKhau.value = ${JSON.stringify(password)}; const c = f.querySelector('[name="xacNhanMatKhau"]'); if (c) c.value = ${JSON.stringify(password)};
      f.querySelectorAll('input[type=checkbox][required]').forEach((x) => { x.checked = true; }); f.requestSubmit(); return true; })()`);
    await customer.go('/login', 400);
    await customer.submit(`(() => { const f = document.querySelector('form[action="/login"]'); f.username.value = ${JSON.stringify(email)}; f.password.value = ${JSON.stringify(password)}; f.requestSubmit(); return true; })()`);
    const cPath = await customer.ev('location.pathname');
    check('customer registered and signed in (storefront, separate context)', !cPath.startsWith('/admin') && !cPath.startsWith('/login'), { path: cPath });

    await customer.go('/account/addresses/new', 400);
    await customer.submit(`(() => { const f = document.querySelector('form.login-card'); f.tenNguoiNhan.value = 'Khách QA P23'; f.soDienThoai.value = '0900000023';
      f.diaChiChiTiet.value = 'Địa chỉ kiểm thử P23'; const m = f.querySelector('[name="laMacDinh"][type=checkbox]'); if (m) m.checked = true; f.requestSubmit(); return true; })()`);

    // Sản phẩm còn hàng đầu tiên trong danh sách.
    await customer.go('/products', 600);
    const pdp = await customer.ev(`(() => { const c = [...document.querySelectorAll('.product-tile')].find((t) => t.querySelector('.badge--success'));
      return c ? c.querySelector('a.store-card').getAttribute('href') : null; })()`);
    check('found an in-stock product', !!pdp, { pdp });
    await customer.go(pdp, 700);
    const truoc = await customer.ev(pdpProbe(null));
    note('pdp before', { pdp, diem: truoc.diem, soDanhGia: truoc.soDanhGia, bars: truoc.bars });

    const datDon = async () => {
      await customer.go(pdp, 500);
      await customer.submit(`(() => { const f = document.querySelector('form[action="/cart/items"]'); if (!f) return false; f.requestSubmit(); return true; })()`);
      await customer.go('/checkout', 600);
      await customer.submit(`(() => { const s = document.querySelector('select[name="addressId"]'); const o = [...s.options].find((x) => x.value);
        s.value = o.value; document.querySelector('[data-update-quote]').click(); return true; })()`);
      await customer.submit(`(() => { const b = [...document.querySelectorAll('[data-place-order]')].find((x) => !x.name && !x.disabled); if (!b) return false; b.click(); return true; })()`, 800);
      const m = (await customer.ev('location.pathname')).match(/^\/orders\/(\d+)$/);
      return m ? Number(m[1]) : null;
    };
    const don1 = await datDon();
    const don2 = await datDon();
    check('two COD orders placed through the UI', !!don1 && !!don2 && don1 !== don2, { don1, don2 });

    // Admin chuyển đơn 1 tới DELIVERED qua đúng nút trạng thái kế tiếp.
    await admin.go(`/admin/orders/${don1}`, 500);
    const buoc = [];
    for (let i = 0; i < 6; i++) {
      const next = await admin.ev(`(() => { const b = document.querySelector('form[action$="/admin/orders/${don1}/status"] button.btn--primary[name="status"]'); return b ? b.value : null; })()`);
      if (!next) break;
      buoc.push(next);
      await admin.submit(`(() => { const b = document.querySelector('form[action$="/admin/orders/${don1}/status"] button.btn--primary[name="status"]'); b.form.requestSubmit(b); return true; })()`, 500);
      if (next === 'DELIVERED') break;
    }
    const adminErr = await admin.ev(`document.querySelector('.form-error, .alert--error')?.textContent.trim() || null`);
    check('Admin moved order 1 to DELIVERED via status buttons', buoc.at(-1) === 'DELIVERED' && !adminErr, { buoc, adminErr });

    // Khách: dòng đơn đã giao có link đánh giá; đơn chưa giao thì không, URL trực tiếp bị 409.
    await customer.go(`/orders/${don1}`, 500);
    const link = await customer.ev(`document.querySelector('a[href^="/orders/items/"][href$="/review"]')?.getAttribute('href') || null`);
    const muc = Number((link || '').match(/items\/(\d+)\//)?.[1]);
    check('delivered order shows review link', !!muc, { link });
    await customer.go(`/orders/${don2}`, 400);
    const link2 = await customer.ev(`!!document.querySelector('a[href^="/orders/items/"][href$="/review"]')`);
    const tryUndelivered = await customer.ev(`Promise.all([fetch('/orders/items/${muc + 1}/review').then((r) => r.status),
      fetch('/orders/items/${muc + 1}/review', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ soSao: '5', _csrf: document.querySelector('input[name=_csrf]')?.value || '' }) }).then((r) => r.status)])`);
    check('undelivered order: no review link, GET/POST review -> 409', !link2 && tryUndelivered[0] === 409 && tryUndelivered[1] === 409, { link2, mucChuaGiao: muc + 1, tryUndelivered });

    // Form đánh giá: radio sao dùng bàn phím, bộ đếm ký tự.
    const token = `QA-P23-${crypto.randomUUID().slice(0, 6)}`;
    await customer.go(link, 500);
    const form0 = await customer.ev(`({ radios: document.querySelectorAll('.star-picker input[type=radio][name=soSao]').length,
      count: document.querySelector('[data-char-count]')?.textContent.replace(/\\s+/g, ' ').trim(),
      maxlength: document.getElementById('noiDung')?.getAttribute('maxlength'), overflowX: document.documentElement.scrollWidth > innerWidth })`);
    check('review form: 5 star radios, 0/1000 counter, maxlength 1000', form0.radios === 5 && form0.count === '0/1000 ký tự' && form0.maxlength === '1000', form0);
    await customer.send('Page.bringToFront');
    await customer.ev(`document.getElementById('soSao1').focus()`);
    for (let i = 0; i < 4; i++) await customer.key('ArrowRight', 'ArrowRight', 39);
    const kb = await customer.ev(`({ checked: document.querySelector('input[name=soSao]:checked')?.value, focused: document.activeElement?.id,
      color5: getComputedStyle(document.querySelector('label[for=soSao5]')).color, color1: getComputedStyle(document.querySelector('label[for=soSao1]')).color })`);
    check('keyboard: ArrowRight x4 selects 5 stars, stars recolor', kb.checked === '5' && kb.focused === 'soSao5', kb);
    const noiDung1 = `${token} thấm nhanh, mùi dễ chịu, da mềm sau 2 tuần.`;
    await customer.ev(`document.getElementById('noiDung').focus()`);
    await customer.send('Input.insertText', { text: noiDung1 });
    await sleep(900);
    const counter = await customer.ev(`document.querySelector('[data-char-current]').textContent`);
    check('character counter follows typing', Number(counter) === noiDung1.length, { counter, expected: noiDung1.length });
    await customer.shot('p23-review-form-1366');
    await customer.viewport(390, 844);
    await sleep(300);
    const ov390 = await customer.overflow();
    check('review form 390px: no horizontal scroll', !ov390);
    await customer.shot('p23-review-form-390');
    await customer.viewport(1366, 768);
    await customer.submit(`(() => { document.querySelector('form.review-form').requestSubmit(); return true; })()`);
    const sau1 = await customer.ev(`({ path: location.pathname, msg: document.querySelector('.notice, .alert--success')?.textContent.trim() })`);
    check('review submitted -> back to order with message', sau1.path === `/orders/${don1}` && /Đã gửi đánh giá/.test(sau1.msg || ''), sau1);

    await customer.go(pdp, 600);
    const choDuyet = await customer.ev(pdpProbe(token));
    check('pending review not public yet', !choDuyet.mine && choDuyet.soDanhGia === truoc.soDanhGia, { mine: choDuyet.mine, soDanhGia: choDuyet.soDanhGia });

    // Admin: lọc chờ duyệt, thấy đúng phiên bản 0, duyệt.
    const adminItem = (t) => `(() => { const li = [...document.querySelectorAll('.admin-review-list li')].find((x) => x.textContent.includes(${JSON.stringify(token)}));
      if (!li) return null; return { badge: li.querySelector('.badge')?.textContent.trim(), revision: li.querySelector('input[name=revision]')?.value,
        button: li.querySelector('button[name=visible]')?.textContent.trim(), text: li.textContent.includes(${JSON.stringify(t || '')}) }; })()`;
    await admin.go('/admin/reviews?status=CHO_DUYET', 600);
    const a1 = await admin.ev(adminItem(''));
    check('Admin: review appears under "Chờ duyệt" with version 0', a1?.badge === 'Chờ duyệt' && a1?.revision === '0', a1);
    await admin.ev(`[...document.querySelectorAll('.admin-review-list li')].find((x) => x.textContent.includes(${JSON.stringify(token)})).scrollIntoView({ block: 'center' })`);
    await admin.shot('p23-admin-reviews-pending-1366');
    await admin.submit(`(() => { const li = [...document.querySelectorAll('.admin-review-list li')].find((x) => x.textContent.includes(${JSON.stringify(token)}));
      const b = li.querySelector('button[name=visible]'); b.form.requestSubmit(b); return true; })()`);
    const duyet1 = await admin.ev(`document.querySelector('.notice')?.textContent.trim() || document.querySelector('.form-error')?.textContent.trim()`);
    check('Admin approve (version 0) succeeds', duyet1 === 'Đã duyệt đánh giá.', { duyet1 });

    await customer.go(pdp, 700);
    const congKhai = await customer.ev(pdpProbe(token));
    check('approved review is public: text, "Đã mua hàng", 5 stars, count +1', !!congKhai.mine && congKhai.mine.badge
      && congKhai.mine.stars === '5 trên 5 sao' && congKhai.mine.text === noiDung1 && congKhai.soDanhGia === truoc.soDanhGia + 1, congKhai);
    note('pdp after approval', { diem: congKhai.diem, starsLabel: congKhai.starsLabel, soDanhGia: congKhai.soDanhGia, bars: congKhai.bars, mine: congKhai.mine });
    await customer.ev(`document.querySelector('.review-summary').scrollIntoView({ block: 'start' }); scrollBy(0, -80)`);
    await customer.shot('p23-pdp-reviews-1366');
    await customer.viewport(390, 844);
    await customer.go(pdp, 600);
    const pdp390 = await customer.ev(pdpProbe(token));
    check('pdp 390px: reviews visible, no horizontal scroll', !!pdp390.mine && !pdp390.overflowX, { overflowX: pdp390.overflowX });
    await customer.ev(`document.querySelector('.review-summary').scrollIntoView({ block: 'start' }); scrollBy(0, -70)`);
    await customer.shot('p23-pdp-reviews-390');
    await customer.viewport(1366, 768);

    // Khách sửa -> ẩn chờ duyệt lại (phiên bản 1).
    const sua = async (soSaoMuon, text) => {
      await customer.go(link, 500);
      await customer.ev(`(() => { const r = document.getElementById('soSao${soSaoMuon}'); r.checked = true; const t = document.getElementById('noiDung'); t.value = ${JSON.stringify(text)}; return true; })()`);
      await customer.submit(`(() => { document.querySelector('form.review-form').requestSubmit(); return true; })()`);
      return customer.ev('location.pathname');
    };
    const formSua = await (async () => { await customer.go(link, 500); return customer.ev(`({ action: document.querySelector('form.review-form').getAttribute('action'),
      checked: document.querySelector('input[name=soSao]:checked')?.value, button: document.querySelector('form.review-form button[type=submit]').textContent.trim() })`); })();
    check('edit form prefilled (5 stars) and posts to /edit', formSua.checked === '5' && formSua.action.endsWith('/review/edit') && formSua.button === 'Lưu và gửi duyệt lại', formSua);
    const noiDung2 = `${token} sửa lần 1: hơi khô vào mùa lạnh.`;
    await sua(3, noiDung2);
    await customer.go(pdp, 600);
    const anLai = await customer.ev(pdpProbe(token));
    check('edit hides the review pending re-approval (count back)', !anLai.mine && anLai.soDanhGia === truoc.soDanhGia, { mine: anLai.mine, soDanhGia: anLai.soDanhGia });

    // @Version tăng ở mọi thay đổi: tạo 0 -> duyệt 1 -> sửa 2. Admin mở danh sách (thấy 2) -> khách sửa tiếp (3)
    // -> Admin bấm duyệt trên trang cũ (2): bị từ chối.
    await admin.go('/admin/reviews?status=CHO_DUYET', 600);
    const a2 = await admin.ev(adminItem(noiDung2));
    check('Admin sees edited review pending again with version 2', a2?.badge === 'Chờ duyệt' && a2?.revision === '2' && a2?.text, a2);
    const noiDung3 = `${token} sửa lần 2: vẫn dùng tiếp, thấm nhanh.`;
    await sua(3, noiDung3);
    await admin.submit(`(() => { const li = [...document.querySelectorAll('.admin-review-list li')].find((x) => x.textContent.includes(${JSON.stringify(token)}));
      const b = li.querySelector('button[name=visible]'); b.form.requestSubmit(b); return true; })()`);
    const stale = await admin.ev(`document.querySelector('.form-error')?.textContent.trim() || null`);
    check('Admin approving a stale version is rejected', /đã thay đổi/.test(stale || ''), { stale });
    await admin.shot('p23-admin-stale-version-1366');
    await admin.go('/admin/reviews?status=CHO_DUYET', 600);
    const a3 = await admin.ev(adminItem(noiDung3));
    await admin.submit(`(() => { const li = [...document.querySelectorAll('.admin-review-list li')].find((x) => x.textContent.includes(${JSON.stringify(token)}));
      const b = li.querySelector('button[name=visible]'); b.form.requestSubmit(b); return true; })()`);
    const duyet3 = await admin.ev(`document.querySelector('.notice')?.textContent.trim() || null`);
    check('Admin re-approves the version they saw (3)', a3?.revision === '3' && duyet3 === 'Đã duyệt đánh giá.', { a3, duyet3 });
    await customer.go(pdp, 600);
    const cuoi = await customer.ev(pdpProbe(token));
    check('public review shows latest edit with 3 stars', cuoi.mine?.text === noiDung3 && cuoi.mine?.stars === '3 trên 5 sao' && cuoi.soDanhGia === truoc.soDanhGia + 1, cuoi.mine);

    // Bộ lọc Admin và 390px.
    await admin.go('/admin/reviews?status=DANG_HIEN&stars=3', 600);
    const loc = await admin.ev(`({ mine: [...document.querySelectorAll('.admin-review-list li')].some((x) => x.textContent.includes(${JSON.stringify(token)})),
      allThree: [...document.querySelectorAll('.admin-review-list .admin-review-meta [aria-label]')].every((s) => s.getAttribute('aria-label') === '3 trên 5 sao'),
      total: document.querySelector('.admin-reviews > p.muted')?.textContent.trim() })`);
    check('Admin filter "Đang hiện" + 3 sao includes the review, only 3-star items', loc.mine && loc.allThree, loc);
    await admin.viewport(390, 844);
    await admin.go('/admin/reviews', 600);
    const aov = await admin.overflow();
    check('Admin reviews 390px: no horizontal scroll', !aov);
    await admin.shot('p23-admin-reviews-390');
    await admin.viewport(1366, 768);

    // Dashboard đọc từ MongoDB.
    await admin.go('/admin/dashboard?range=30d', 1200);
    const dash = await admin.ev(`(() => { const li = [...document.querySelectorAll('.todo-count')].map((s) => ({ n: s.textContent.trim(), label: s.parentElement.textContent.replace(/\\s+/g, ' ').trim() }));
      const rows = [...document.querySelectorAll('table tr')].filter((r) => /\\d sao/.test(r.textContent)).map((r) => r.textContent.replace(/\\s+/g, ' ').trim());
      return { todo: li, sao: rows }; })()`);
    note('dashboard (30d)', dash);
    check('Dashboard shows review counters', dash.todo.some((t) => /đánh giá/i.test(t.label)) && dash.sao.length === 5, dash);

    // Thẻ sản phẩm hiện sao trung bình (một truy vấn tóm tắt cho cả trang).
    await customer.go('/products', 800);
    const cards = await customer.ev(`({ cards: document.querySelectorAll('.product-tile').length, rated: document.querySelectorAll('.store-card-rating').length,
      sample: [...document.querySelectorAll('.store-card-rating')].slice(0, 3).map((p) => p.textContent.replace(/\\s+/g, ' ').trim()),
      overflowX: document.documentElement.scrollWidth > innerWidth })`);
    check('/products cards show average stars + count', cards.rated > 0 && !cards.overflowX, cards);
    await customer.ev(`document.querySelector('.store-card-rating')?.closest('.product-tile')?.scrollIntoView({ block: 'center' })`);
    await customer.shot('p23-products-card-rating-1366');
    await customer.go('/', 1500);
    const home = await customer.ev(`({ popular: [...document.querySelectorAll('#home-popular .product-tile')].map((t) => ({
      href: t.querySelector('a.store-card')?.getAttribute('href'), rating: t.querySelector('.store-card-rating')?.textContent.replace(/\\s+/g, ' ').trim() || null })) })`);
    note('home popular (Mongo-backed reviews + demo wishlists)', home.popular);
    check('home popular section renders', home.popular.length > 0, home.popular.length);

    results.exceptions = [...admin.exceptions, ...customer.exceptions];
    results.ids = { don1, don2, muc, pdp };
  }
} catch (e) {
  failures++;
  results.error = String(e?.stack || e);
  console.log('ERROR', e);
} finally {
  results.failures = failures;
  results.finishedAt = new Date().toISOString();
  fs.writeFileSync(path.join(qaDir, `results-${mode}.json`), JSON.stringify(results, null, 2));
  console.log(`${failures === 0 ? 'ALL PASS' : failures + ' FAIL'}; page exceptions: ${(results.exceptions || []).length}`);
  proc.kill();
  await sleep(500);
  try { fs.rmSync(profile, { recursive: true, force: true }); } catch { /* ignore */ }
  process.exit(failures === 0 ? 0 : 1);
}
