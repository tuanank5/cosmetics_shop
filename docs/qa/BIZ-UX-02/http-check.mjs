import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';

const [origin, phase] = process.argv.slice(2);
assert.equal(new URL(origin).hostname, '127.0.0.1');
const output = { phase, mode: 'HTTP/SSR, no browser', screens: [], checks: {}, performance: {} };

function session() {
  const cookies = new Map();
  return async (route, form) => {
    const url = new URL(route, origin);
    assert.equal(url.origin, origin);
    const start = performance.now();
    const response = await fetch(url, {
      method: form == null ? 'GET' : 'POST',
      redirect: 'manual',
      headers: { Accept: 'text/html', Cookie: [...cookies].map(([key, value]) => key + '=' + value).join('; ') },
      body: form instanceof FormData ? form : form ? new URLSearchParams(form) : undefined,
    });
    const headersMs = performance.now() - start;
    for (const cookie of response.headers.getSetCookie()) {
      const [key, value] = cookie.split(';')[0].split('=');
      if (value) cookies.set(key, value);
      else cookies.delete(key);
    }
    return { status: response.status, location: response.headers.get('location'), html: await response.text(), headersMs };
  };
}
const csrf = ({ html }) => {
  const field = html.match(/name="_csrf" value="([^"]+)"/)?.[1];
  assert.ok(field, 'Expected a server-rendered CSRF field');
  return field;
};
const routeOf = route => new URL(route, origin).pathname;
async function login(request, name) {
  const loginPage = await request('/login');
  const access = await request('/login', {
    username: name + '@example.invalid', password: process.env.BIZ_UX02_PASSWORD, _csrf: csrf(loginPage),
  });
  assert.equal(access.status, 302);
  assert.notEqual(routeOf(access.location), '/login');
  return routeOf(access.location);
}
async function scan(request, role, routes) {
  for (const [route, expected = 200] of routes) {
    const page = await request(route);
    const matches = [...page.html.matchAll(/<([a-z][\w:-]*)\b[^>]*\bid="([^"]+)"/gi)];
    const ids = matches.map(match => match[2]);
    const labels = [...page.html.matchAll(/<label\b[^>]*\bfor="([^"]+)"/gi)].map(match => match[1]);
    const references = [...page.html.matchAll(/\baria-(?:labelledby|describedby)="([^"]+)"/gi)]
      .flatMap(match => match[1].split(/\s+/));
    const stats = {
      role, route, status: page.status, h1: (page.html.match(/<h1\b/gi) || []).length,
      skipTarget: page.html.includes('href="#main"') && ids.includes('main'),
      duplicateIds: [...new Set(ids.filter((id, index) => ids.indexOf(id) !== index))],
      unresolvedLabels: labels.filter(id => !ids.includes(id)),
      unresolvedAria: [...new Set(references.filter(id => !ids.includes(id)))],
    };
    output.screens.push(stats);
    assert.equal(page.status, expected, role + ' ' + route);
  }
}
async function measure(request, route) {
  const page = await request(route);
  assert.equal(page.status, 200);
  const scripts = [...page.html.matchAll(/<script\b[^>]*\bsrc="([^"]+)"/gi)].map(match => match[1]);
  const css = [...page.html.matchAll(/<link\b[^>]*rel="stylesheet"\s+href="([^"]+)"/gi)].map(match => match[1]);
  const tags = [...scripts, ...css].filter(url => url.startsWith('/') && !url.startsWith('//'));
  const assets = {};
  for (const asset of new Set(tags)) {
    const result = await request(asset);
    assets[asset] = { status: result.status, bytes: Buffer.byteLength(result.html) };
  }
  const headersSamples = [];
  for (let i = 0; i < 7; i++) headersSamples.push(Math.round((await request(route)).headersMs * 10) / 10);
  const ordered = [...headersSamples].sort((a, b) => a - b);
  return { tags: tags.length, duplicateAssetTags: tags.length - Object.keys(assets).length, assets,
    htmlBytes: Buffer.byteLength(page.html), elementStartTags: (page.html.match(/<[a-z][\w:-]*(?=[ />])/gi) || []).length,
    headersSamples, medianHeadersMs: ordered[3] };
}

try {
  const guest = session();
  await scan(guest, 'guest', [
    ['/'], ['/products'], ['/products?category=serum&sort=priceAsc'],
    ['/products/fixture-serum?variant=1'], ['/products/fixture-serum?variant=2'],
    ['/cart'], ['/login'], ['/register'], ['/forgot-password'], ['/chat'], ['/tu-van-da'],
    ['/products/does-not-exist', 404], ['/checkout', 302],
  ]);
  const home = await guest('/');
  assert.equal((home.html.match(/<section [^>]*\bhome-slide\b/g) || []).length, 6);
  const chapter = home.html.match(/<section id="home-advisor"[\s\S]*?<\/section>/)?.[0] || '';
  output.checks.homeAdvice = { faq: /href="\/chat"/.test(chapter), skin: /href="\/tu-van-da"/.test(chapter) };
  assert.equal(output.checks.homeAdvice.faq, true);
  assert.equal(output.checks.homeAdvice.skin, phase === 'after');
  output.performance.home = await measure(guest, '/');
  output.performance.catalog = await measure(guest, '/products');

  const filtered = await guest('/products?category=serum&sort=priceAsc');
  assert.match(filtered.html, /fixture-serum/);
  const item = await guest('/products/fixture-serum?variant=1');
  assert.match(item.html, /QA-SERUM-30/);
  assert.equal((await guest('/cart/items', { _csrf: csrf(item), variant: '1' })).status, 302);
  assert.equal((await guest('/checkout')).status, 302);
  assert.equal(await login(guest, 'buyer'), '/checkout');
  const mergedCart = await guest('/cart');
  assert.match(mergedCart.html, /QA-SERUM-30/);
  const address = await guest('/account/addresses/new?from=checkout');
  assert.equal(routeOf((await guest('/account/addresses/new', {
    _csrf: csrf(address), from: 'checkout', tenNguoiNhan: 'Fixture Buyer', soDienThoai: '0000000000',
    diaChiChiTiet: 'Fixture shipping address', laMacDinh: 'true',
  })).location), '/checkout');
  const unquoted = await guest('/checkout');
  const quote = await guest('/checkout/quote', { _csrf: csrf(unquoted), addressId: '1', voucher: 'FIXTURE10' });
  assert.equal(quote.status, 200);
  assert.match(quote.html, /data-quote-valid="true"/);
  assert.match(quote.html, /data-checkout-total[^>]*>.*165\.000 ₫/);
  const invalid = await guest('/checkout/quote', { _csrf: csrf(unquoted), addressId: '1', voucher: 'WRONG-CODE' });
  assert.match(invalid.html, /data-quote-valid="false"/);
  assert.match(invalid.html, /value="WRONG-CODE"/);
  const checkoutPost = { _csrf: csrf(quote), addressId: '1', voucher: 'FIXTURE10' };
  const placed = await Promise.all([guest('/checkout', checkoutPost), guest('/checkout', checkoutPost)]);
  assert.deepEqual(placed.map(page => page.status).sort(), [200, 302]);
  const order = routeOf(placed.find(page => page.status === 302).location);
  assert.match(order, /^\/orders\/\d+$/);
  await scan(guest, 'customer', [
    ['/cart'], ['/account'], ['/account/addresses'], ['/account/security'], ['/account/wishlist'],
    ['/orders'], [order], ['/account/returns'], ['/admin/dashboard', 403],
  ]);
  output.checks.cod = { result: 'PASS', quote: 165000, postStatuses: [200, 302], voucherPreservedOnError: true };

  const admin = session();
  assert.equal(await login(admin, 'admin'), '/admin/dashboard');
  const skuPath = '/admin/catalog/products/1/variants/new';
  await scan(admin, 'admin', [
    ['/admin/dashboard'], ['/admin/orders'], ['/admin' + order], ['/admin/catalog/products'],
    ['/admin/catalog/products/1/variants'], [skuPath], ['/admin/catalog/categories'],
    ['/admin/catalog/brands'], ['/admin/returns'], ['/admin/vouchers'], ['/admin/users'],
    ['/admin/reviews'], ['/admin/ai'],
  ]);
  output.performance.dashboard = await measure(admin, '/admin/dashboard');
  const sku = await admin(skuPath);
  const badSku = new FormData();
  for (const [name, value] of Object.entries({
    _csrf: csrf(sku), sku: 'QA-INVALID', tenBienThe: 'Fixture size', giaBan: '-1',
    soLuongTrongKho: '10', dangHoatDong: 'true',
  })) badSku.set(name, value);
  const skuErrors = await admin(skuPath, badSku);
  assert.equal(skuErrors.status, 200);
  const priceInput = skuErrors.html.match(/<input[^>]*\bid="giaBan"[^>]*>/)?.[0] || '';
  assert.ok(priceInput);
  output.checks.skuError = { name: 'giaBan', ariaInvalid: priceInput.includes('aria-invalid="true"'),
    errorDescribed: priceInput.includes('aria-describedby="sku-price-error"') && skuErrors.html.includes('id="sku-price-error"') };
  assert.equal(output.checks.skuError.ariaInvalid, phase === 'after');
  assert.equal(output.checks.skuError.errorDescribed, phase === 'after');

  const detail = await admin('/admin' + order);
  const changed = await admin('/admin' + order + '/status', { _csrf: csrf(detail), status: 'CONFIRMED' });
  assert.equal(changed.status, 302);
  assert.match((await admin('/admin' + order)).html, /value="PACKING"/);
  output.checks.adminConfirmation = 'PASS';
  const skin = await guest('/tu-van-da');
  const unavailable = await guest('/tu-van-da', {
    _csrf: csrf(skin), loaiDa: 'Da khô', vanDeDa: 'Tôi cần serum.',
    daNhayCam: 'Ít nhạy cảm', nhuCau: 'Dưỡng ẩm',
  });
  assert.equal(unavailable.status, 200);
  assert.match(unavailable.html, /AI tư vấn hiện chưa sẵn sàng/);
  output.checks.skinProviderDisabled = 'PASS';
  output.result = 'PASS';
} catch (error) {
  output.result = 'FAIL';
  output.error = String(error.message).split('\n')[0].slice(0, 150);
  process.exitCode = 1;
} finally {
  await writeFile('docs/qa/BIZ-UX-02/' + phase + '-http.json', JSON.stringify(output, null, 2) + '\n');
  console.log(output.result, phase, output.screens.length, output.error || '');
}
