import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';
import path from 'node:path';

const [base, , phase] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
const evidence = { phase, checks: [], assetBytes: {} };

function session() {
  const cookies = new Map();
  return async (route, form) => {
    const url = new URL(route, base);
    assert.equal(url.origin, base);
    const response = await fetch(url, {
      method: form ? 'POST' : 'GET',
      redirect: 'manual',
      headers: { Cookie: [...cookies].map(([name, value]) => name + '=' + value).join('; ') },
      body: form ? new URLSearchParams(form) : undefined,
    });
    for (const cookie of response.headers.getSetCookie()) {
      const [name, value] = cookie.split(';')[0].split('=');
      if (value) cookies.set(name, value);
      else cookies.delete(name);
    }
    return { status: response.status, location: response.headers.get('location'), html: await response.text() };
  };
}

const csrf = (page) => {
  const value = page.html.match(/name="_csrf" value="([^"]+)"/)?.[1];
  assert.ok(value, 'Expected an SSR CSRF field');
  return value;
};
const assertStatus = (page, expected) => assert.equal(page.status, expected);
const record = (name, details = {}) => evidence.checks.push({ name, ...details });

try {
  const buyer = session();
  const home = await buyer('/');
  assertStatus(home, 200);
  const advisor = home.html.match(/href="([^"]+)">Hỏi BeautyShop/)?.[1];
  assert.equal(advisor, phase === 'before' ? '/tu-van-da' : '/chat');
  for (const route of ['/chat', '/products', '/products/fixture-a?variant=3']) {
    assertStatus(await buyer(route), 200);
  }
  const skin = await buyer('/tu-van-da');
  assertStatus(skin, 200);
  const dataHint = skin.html.includes('id="skin-advice-data"');
  assert.equal(dataHint, phase === 'after');
  const footer = home.html.match(/<footer class="site-footer">([\s\S]+?)<\/footer>/)?.[1] || '';
  const links = [...footer.matchAll(/href="([^"]+)">\s*([^<]+?)\s*<\/a>/g)]
    .map(([, href, label]) => ({ href, label }));
  assert.equal(links.some(link => /Chính sách bảo mật thông tin/.test(link.label)), phase === 'before');
  const guest = await buyer('/products');
  for (const [, route] of guest.html.matchAll(/<link [^>]*rel="stylesheet" href="(\/css\/[^"]+)"/g)) {
    const asset = await buyer(route);
    assertStatus(asset, 200);
    evidence.assetBytes[route] = Buffer.byteLength(asset.html);
  }
  record('guest-catalog-and-advice', { advisor, dataHint, footerLinks: links });
  assertStatus(await buyer('/cart/items', { variant: '1' }), 403);
  const pdp = await buyer('/products/fixture-a?variant=1');
  assertStatus(pdp, 200);
  assert.match(pdp.html, /FIXTURE-A/);
  assertStatus(await buyer('/cart/items', { _csrf: csrf(pdp), variant: '1' }), 302);
  const cart = await buyer('/cart');
  assertStatus(cart, 200);
  assert.match(cart.html, /100\.000 ₫/);
  assertStatus(await buyer('/cart/items/1/quantity', { _csrf: csrf(cart), quantity: '2' }), 302);
  const updated = await buyer('/cart');
  assert.match(updated.html, /200\.000 ₫/);
  assert.match(updated.html, /data-cart-line="1"/);
  const protectedCheckout = await buyer('/checkout');
  assertStatus(protectedCheckout, 302);
  assert.equal(new URL(protectedCheckout.location, base).pathname, '/login');
  await login(buyer, 'buyer');
  const checkout = await buyer('/checkout');
  assertStatus(checkout, 200);
  assert.match(checkout.html, /FIXTURE-A/);
  assert.match(checkout.html, /200\.000 ₫/);
  const addressHint = checkout.html.includes('id="address-manage-hint"');
  assert.equal(addressHint, phase === 'after');
  const addressForm = await buyer('/account/addresses/new?from=checkout');
  assertStatus(addressForm, 200);
  const madeAddress = await buyer('/account/addresses/new', {
    _csrf: csrf(addressForm), from: 'checkout', tenNguoiNhan: 'Fixture Receiver',
    soDienThoai: '0000000000', diaChiChiTiet: 'Fixture delivery address', laMacDinh: 'true',
  });
  assertStatus(madeAddress, 302);
  assert.equal(new URL(madeAddress.location, base).pathname, '/checkout');
  const addressCheckout = await buyer('/checkout');
  const addressId = addressCheckout.html.match(/value="(\d+)" selected="selected"[^>]*>Fixture Receiver/)?.[1];
  assert.ok(addressId, 'New fixture address is the checkout default');
  assertStatus(await buyer('/admin/orders'), 403);
  assertStatus(await buyer('/account/addresses/999999/edit?from=checkout'), 404);
  record('cart-form-csrf-merge-and-address', { cartSubtotal: 200000, addressHint });

  const quote = await buyer('/checkout/quote', {
    _csrf: csrf(addressCheckout), addressId, voucher: '', subtotal: '1', price: '1',
  });
  assertStatus(quote, 200);
  assert.match(quote.html, /data-quote-valid="true"/);
  assert.match(quote.html, /data-checkout-total[^>]*>.*225\.000 ₫/);
  const orderPost = { _csrf: csrf(quote), addressId, voucher: '', total: '1', price: '1' };
  const checkoutResults = await Promise.all([buyer('/checkout', orderPost), buyer('/checkout', orderPost)]);
  const placed = checkoutResults.find(result => result.status === 302);
  assert.deepEqual(checkoutResults.map(result => result.status).sort(), [200, 302]);
  const orderPath = new URL(placed.location, base).pathname;
  assert.match(orderPath, /^\/orders\/\d+$/);
  const order = await buyer(orderPath);
  assertStatus(order, 200);
  assert.match(order.html, /225\.000 ₫/);
  assert.match(order.html, /Fixture delivery address/);
  const customerReturns = order.html.match(/href="\/account\/returns">([^<]+)/)?.[1];
  assert.equal(customerReturns, phase === 'before' ? 'Chính sách đổi trả và hoàn tiền' : 'Yêu cầu đổi trả');
  assertStatus(await buyer('/account/returns'), 200);
  record('quote-cod-concurrent-double-submit-and-owner-view', {
    quote: 225000, statuses: checkoutResults.map(result => result.status).sort(),
    customerReturns,
  });

  const other = session();
  await login(other, 'other');
  assertStatus(await other(orderPath), 404);
  assertStatus(await other('/account/addresses/' + addressId + '/edit'), 404);
  const admin = session();
  const target = await login(admin, 'admin');
  assert.equal(new URL(target, base).pathname, '/admin/dashboard');
  assertStatus(await admin('/admin/dashboard'), 200);
  const adminList = await admin('/admin/orders?status=PENDING_CONFIRMATION&status=CONFIRMED&size=10');
  assertStatus(adminList, 200);
  assert.match(adminList.html, /href="\/admin\/catalog\/categories">Quản lý danh mục/);
  assert.doesNotMatch(adminList.html, /href="\/account\/returns">/);
  const adminRoute = '/admin' + orderPath;
  const adminOrder = await admin(adminRoute);
  assertStatus(adminOrder, 200);
  assert.match(adminOrder.html, /value="CONFIRMED"/);
  const changed = await admin(adminRoute + '/status', { _csrf: csrf(adminOrder), status: 'CONFIRMED' });
  assertStatus(changed, 302);
  assert.match((await admin(adminRoute)).html, /value="PACKING"/);
  assertStatus(await admin('/checkout'), 403);
  record('customer-ownership-and-admin-confirmation');
} catch (error) {
  evidence.error = error.message.split('\n')[0].slice(0, 200);
  throw error;
} finally {
  await writeFile(path.resolve('docs/qa/BIZ-UX-01', phase + '-http.json'), JSON.stringify(evidence, null, 2) + '\n');
}

async function login(request, role) {
  const page = await request('/login');
  assertStatus(page, 200);
  const posted = await request('/login', { _csrf: csrf(page), username: role + '@example.invalid', password: process.env.BIZ_UX_FIXTURE_PASS });
  assertStatus(posted, 302);
  assert.notEqual(new URL(posted.location, base).pathname, '/login');
  return posted.location;
}
