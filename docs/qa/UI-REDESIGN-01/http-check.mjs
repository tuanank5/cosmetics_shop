import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';

const [origin, revision, output] = process.argv.slice(2);
assert.equal(new URL(origin).hostname, '127.0.0.1');
assert.ok(['before', 'after'].includes(revision));
const after = revision === 'after';
const proof = { revision, method: 'HTTP with real Spring Boot and isolated databases', checks: {}, pages: {} };
const image = '/uploads/sku/aa123456-1234-1234-1234-123456789abc.png';

function session() {
  const cookies = new Map();
  return async (route, fields) => {
    const url = new URL(route, origin);
    assert.equal(url.origin, origin);
    const response = await fetch(url, {
      method: fields ? 'POST' : 'GET',
      redirect: 'manual',
      headers: { Cookie: [...cookies].map(([key, value]) => key + '=' + value).join('; ') },
      body: fields ? new URLSearchParams(fields) : undefined,
    });
    for (const cookie of response.headers.getSetCookie()) {
      const [key, value] = cookie.split(';')[0].split('=');
      if (value) cookies.set(key, value);
      else cookies.delete(key);
    }
    return {
      status: response.status,
      html: await response.text(),
      path: response.headers.get('location') ? new URL(response.headers.get('location'), origin).pathname : null,
      type: response.headers.get('content-type'),
    };
  };
}
function csrf(page) {
  const token = page.html.match(/name="_csrf" value="([^"]+)"/)?.[1];
  assert.ok(token, 'Server-rendered CSRF token is required');
  return token;
}
function expectStatus(page, status) {
  assert.equal(page.status, status);
  return page;
}
function structure(role, route, page) {
  const ids = [...page.html.matchAll(/\sid="([^"]+)"/g)].map(match => match[1]);
  const labelTargets = [...page.html.matchAll(/<label\b[^>]*\sfor="([^"]+)"/g)].map(match => match[1]);
  const ariaTargets = [...page.html.matchAll(/\saria-(?:labelledby|describedby)="([^"]+)"/g)]
    .flatMap(match => match[1].split(/\s+/));
  const row = proof.pages[role + ' ' + route] = {
    status: page.status, headings: [...page.html.matchAll(/<h1(?:\s[^>]*)?>(.*?)<\/h1>/gs)].map(m => m[1]),
    skipTarget: page.html.includes('href="#main"') && ids.includes('main'),
    duplicateIds: [...new Set(ids.filter((id, i) => ids.indexOf(id) !== i))],
    unresolvedLabels: labelTargets.filter(id => !ids.includes(id)),
    unresolvedAria: [...new Set(ariaTargets.filter(id => !ids.includes(id)))],
  };
  if (after) {
    assert.deepEqual(row.duplicateIds, [], 'Duplicate IDs on ' + route);
    assert.deepEqual(row.unresolvedLabels, [], 'Broken label on ' + route);
    assert.deepEqual(row.unresolvedAria, [], 'Broken ARIA ID on ' + route);
    assert.equal(row.skipTarget, true);
    assert.equal(row.headings.length, 1);
  }
}
const header = html => html.match(/<header\b[\s\S]*?<\/header>/)?.[0] || '';
async function login(client, name) {
  const page = expectStatus(await client('/login'), 200);
  const result = expectStatus(await client('/login', {
    username: name + '@example.invalid',
    password: process.env.UI_REDESIGN_FIXTURE_PASSWORD,
    _csrf: csrf(page),
  }), 302);
  assert.notEqual(result.path, '/login');
}
function headerSummary(html) {
  const nav = header(html);
  const actions = nav.indexOf('class="header-utilities"');
  const auth = Math.max(nav.indexOf('>Đăng nhập</a>'), nav.indexOf('>Đăng xuất</button>'));
  return {
    authInUtilities: actions >= 0 && auth > actions,
    loginAction: (nav.match(/href="\/login"/g) || []).length,
    logoutPost: (nav.match(/method="post" action="\/logout"/g) || []).length,
    closeDiscoveryBeforeGroups: nav.includes('data-close-discovery')
      && nav.indexOf('data-close-discovery') < nav.indexOf('class="nav-group"'),
  };
}
let step = 'home';

try {
  const customer = session();
  const home = expectStatus(await customer('/'), 200);
  structure('guest', '/', home);
  const guestHeader = headerSummary(home.html);
  assert.equal((home.html.match(/<section [^>]*\bhome-slide\b/g) || []).length, 6);
  const cta = home.html.match(/<section id="home-advisor"[\s\S]*?<\/section>/)?.[0] || '';
  assert.match(cta, /href="\/chat"/);
  if (after) {
    assert.doesNotMatch(home.html, /href="\/tu-van-da"/);
    assert.deepEqual(guestHeader, { authInUtilities: true, loginAction: 1, logoutPost: 0, closeDiscoveryBeforeGroups: true });
  }

  step = 'chat';
  const chat = expectStatus(await customer('/chat'), 200);
  structure('guest', '/chat', chat);
  const former = expectStatus(await customer('/tu-van-da'), after ? 302 : 200);
  const faq = expectStatus(await customer('/chat', { _csrf: csrf(chat), cauHoi: 'Chính sách đổi trả thế nào?' }), 200);
  if (after) {
    assert.equal(former.path, '/chat');
    assert.match(chat.html, /BeautyShop AI/);
    assert.doesNotMatch(chat.html, /<select\b/);
    assert.match(faq.html, /AI hiện chưa sẵn sàng/);
    assert.match(faq.html, /chat-message--user/);
    expectStatus(await customer('/tu-van-da', { vanDeDa: 'Fixture ignored' }), 403);
    assert.equal(expectStatus(await customer('/tu-van-da', { _csrf: csrf(chat), vanDeDa: 'Fixture ignored' }), 303).path, '/chat');
  }
  proof.checks.chat = { legacyGet: former.status, unified: after, faqWithoutOllama: faq.status === 200 };

  step = 'wishlist';
  const item = expectStatus(await customer('/products/fixture-serum?variant=1'), 200);
  assert.match(item.html, /QA-SERUM-30/);
  expectStatus(await customer('/cart/items', { _csrf: csrf(item), variant: '1' }), 302);
  expectStatus(await customer('/checkout'), 302);
  await login(customer, 'buyer');
  expectStatus(await customer('/account/wishlist/1', { _csrf: csrf(await customer('/account/wishlist')) }), 302);
  expectStatus(await customer('/account/wishlist/2', { _csrf: csrf(await customer('/account/wishlist')) }), 302);
  const wishlist = expectStatus(await customer('/account/wishlist'), 200);
  structure('customer', '/account/wishlist', wishlist);
  assert.match(wishlist.html, /Fixture Serum with Long Product Name/);
  const wishData = { image: wishlist.html.includes(image), price: wishlist.html.includes('150.000 ₫'),
    stock: wishlist.html.includes('Còn hàng'), outOfStock: wishlist.html.includes('Hết hàng'),
    brand: wishlist.html.includes('Fixture Brand'), variants: wishlist.html.includes('2 biến thể') };
  if (after) assert.ok(Object.values(wishData).every(Boolean));
  const asset = await customer(image);
  proof.checks.imageResponse = { status: asset.status, type: asset.type };
  assert.match(expectStatus(asset, 200).type, /^image\/png(?:;|$)/i);
  const other = session();
  await login(other, 'other');
  const otherWishlist = expectStatus(await other('/account/wishlist'), 200);
  assert.doesNotMatch(otherWishlist.html, /Fixture Serum with Long Product Name/);
  proof.checks.wishlist = { ...wishData, ownerOnly: true };

  step = 'checkout';
  const checkout = expectStatus(await customer('/checkout'), 200);
  structure('customer', '/checkout', checkout);
  assert.doesNotMatch(checkout.html, /Địa chỉ riêng của tài khoản khác/);
  const controls = { initialQuote: checkout.html.includes('data-quote-valid="true"'),
    oldRecalculateButton: checkout.html.includes('>Tính lại tổng tiền</button>'),
    stableOrderForm: checkout.html.includes('id="checkout-order"'), image: checkout.html.includes(image) };
  assert.equal(controls.initialQuote, after);
  assert.equal(controls.oldRecalculateButton, !after);
  if (after) assert.match(checkout.html, /data-checkout-total[^>]*>[^\n]*175\.000 ₫/);
  const form = (addressId, voucher) => ({ _csrf: csrf(checkout), addressId, voucher });
  const quote = async (addressId, voucher) => expectStatus(await customer('/checkout/quote', form(addressId, voucher)), 200);
  const valid = await quote('2', 'FIXTURE10');
  assert.match(valid.html, /data-quote-valid="true"/);
  assert.match(valid.html, /data-checkout-total[^>]*>[^\n]*165\.000 ₫/);
  if (after) assert.match(valid.html, /Giao đến Fixture thay thế/);
  const cleared = await quote('1', '');
  assert.match(cleared.html, /data-checkout-total[^>]*>[^\n]*175\.000 ₫/);
  for (const [code, message] of [
    ['WRONG-CODE', /không hợp lệ/],
    ['FIXTUREEXPIRED', /đã hết hạn/],
    ['FIXTUREMIN', /chưa đạt mức tối thiểu/],
  ]) {
    const invalid = await quote('2', code);
    assert.match(invalid.html, /data-quote-valid="false"/);
    assert.match(invalid.html, new RegExp('value="' + code + '"'));
    assert.match(invalid.html, message);
    assert.doesNotMatch(invalid.html, /data-checkout-total/);
  }
  assert.match((await quote('3', '')).html, /data-quote-valid="false"/);
  expectStatus(await customer('/checkout/quote', { addressId: '2' }), 403);
  expectStatus(await customer('/checkout', { addressId: '2' }), 403);
  proof.checks.checkout = { ...controls, secondAddress: true, voucherCases: 5, totalWithVoucher: 165000 };

  step = 'order';
  const placed = await Promise.all([customer('/checkout', form('2', 'FIXTURE10')), customer('/checkout', form('2', 'FIXTURE10'))]);
  const postStatuses = placed.map(page => page.status).sort();
  assert.deepEqual(postStatuses, [200, 302]);
  const orderPath = placed.find(page => page.status === 302).path;
  assert.match(orderPath, /^\/orders\/\d+$/);
  const order = expectStatus(await customer(orderPath), 200);
  structure('customer', '/orders/{id}', order);
  assert.match(order.html, /165\.000 ₫/);
  assert.match(order.html, /Địa chỉ thử thứ hai/);
  expectStatus(await other(orderPath), 404);
  const detail = { timeline: order.html.includes('order-timeline'), image: order.html.includes(image),
    cancelConfirmation: order.html.includes('Xác nhận hủy đơn'), postStatuses, ownerOnly: true };
  if (after) assert.ok(detail.timeline && detail.image && detail.cancelConfirmation);
  proof.checks.order = detail;

  step = 'roles';
  const admin = session();
  await login(admin, 'admin');
  const dashboard = expectStatus(await admin('/admin/dashboard'), 200);
  structure('admin', '/admin/dashboard', dashboard);
  expectStatus(await admin('/checkout'), 403);
  expectStatus(await customer('/admin/dashboard'), 403);
  const customerHeader = headerSummary(checkout.html);
  const adminHeader = headerSummary(dashboard.html);
  if (after) {
    assert.equal(customerHeader.authInUtilities, true);
    assert.equal(adminHeader.authInUtilities, true);
    assert.equal(customerHeader.loginAction, 0);
    assert.equal(customerHeader.logoutPost, 1);
    assert.equal(adminHeader.logoutPost, 1);
  }
  expectStatus(await customer('/logout', {}), 403);
  expectStatus(await customer('/account/wishlist'), 200);
  expectStatus(await customer('/logout', { _csrf: csrf(checkout) }), 302);
  expectStatus(await customer('/account/wishlist'), 302);
  proof.checks.roles = { guestHeader, customerHeader, adminHeader, csrfLogout: true };
  proof.status = 'PASS';
} catch (error) {
  proof.status = 'FAIL';
  proof.failedStep = step;
  proof.failedLine = error.stack?.match(/http-check\.mjs:(\d+)/)?.[1] || 'unknown';
  console.error('HTTP assertion failed at ' + step + ', line ' + proof.failedLine + ': ' + error.name);
  process.exitCode = 1;
} finally {
  await writeFile(output, JSON.stringify(proof, null, 2) + '\n');
}
