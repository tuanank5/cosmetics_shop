import assert from 'node:assert/strict';
import { writeFile } from 'node:fs/promises';

const [origin, revision, output] = process.argv.slice(2);
assert.equal(new URL(origin).hostname, '127.0.0.1');
const after = revision === 'after';
const result = { revision, checks: {} };
const image = '/uploads/sku/aa123456-1234-1234-1234-123456789abc.png';
const address = '12 Nguyễn Văn Bảo, Gò Vấp, Thành phố Hồ Chí Minh, Việt Nam';
let step = 'footer';

function session() {
  const cookies = new Map();
  return async (path, fields, status = 200) => {
    const url = new URL(path, origin);
    assert.equal(url.origin, origin);
    const response = await fetch(url, {
      method: fields ? 'POST' : 'GET',
      redirect: 'manual',
      headers: { Cookie: [...cookies].map(([k, v]) => k + '=' + v).join('; ') },
      body: fields ? new URLSearchParams(fields) : undefined,
    });
    for (const cookie of response.headers.getSetCookie()) {
      const [key, value] = cookie.split(';')[0].split('=');
      if (value) cookies.set(key, value);
      else cookies.delete(key);
    }
    assert.equal(response.status, status, path);
    return { html: await response.text(), location: response.headers.get('location') };
  };
}

function csrf(html) {
  const token = html.match(/name="_csrf" value="([^"]+)"/)?.[1];
  assert.ok(token, 'Thiếu CSRF');
  return token;
}

async function login(client, username) {
  const page = await client('/login');
  const login = await client('/login', { username: username + '@example.invalid',
    password: process.env.ADMIN_UX_FIXTURE_PASSWORD, _csrf: csrf(page.html) }, 302);
  assert.doesNotMatch(login.location, /login\?error/);
}

const footer = html => html.match(/<footer\b[\s\S]*?<\/footer>/)?.[0] || '';
const table = html => /<table class="admin-table(?:[ "])/.test(html);
function labels(html) {
  const ids = [...html.matchAll(/\sid="([^"]+)"/g)].map(match => match[1]);
  assert.equal(new Set(ids).size, ids.length, 'ID HTML bị trùng');
  for (const [, value] of html.matchAll(/\s(?:for|aria-labelledby|aria-describedby)="([^"]+)"/g)) {
    for (const id of value.split(/\s+/)) assert.ok(ids.includes(id), 'Nhãn HTML thiếu đích');
  }
}

try {
  const buyer = session();
  const shop = await buyer('/products');
  const lienHe = footer(shop.html);
  if (after) {
    assert.ok(lienHe.includes(address + '.'));
    assert.match(lienHe, /<iframe[^>]+google\.com\/maps[\s\S]+?title="Google Maps[^"]+" loading="lazy"/);
    assert.match(lienHe, /maps\/search\/\?api=1&amp;query=[^"]+/);
    assert.equal(decodeURIComponent(lienHe.match(/output=embed&amp;q=([^"]+)"/)[1]), address);
    if (process.env.ADMIN_UX_CONTACT_FIXTURE) {
      for (const link of ['tel:', 'mailto:', 'zalo.me/', 'facebook.com/', 'instagram.com/']) assert.ok(lienHe.includes(link));
      assert.equal((lienHe.match(/rel="noopener noreferrer"/g) || []).length, 4);
    } else {
      assert.doesNotMatch(lienHe, /tel:|mailto:|https:\/\/[^"]*(?:zalo\.me|facebook\.com|instagram\.com)/);
    }
    labels(shop.html);
  }
  result.checks.footer = { embed: /<iframe/.test(lienHe), address: lienHe.includes(address),
    fallback: lienHe.includes('Xem trên Google Maps'), configuredContact: !!process.env.ADMIN_UX_CONTACT_FIXTURE };

  step = 'checkout';
  await login(buyer, 'buyer');
  await buyer('/cart/items', { variant: '1', _csrf: csrf((await buyer('/cart')).html) }, 302);
  const checkout = await buyer('/checkout');
  const placed = await buyer('/checkout', { addressId: '2', voucher: 'FIXTURE10', _csrf: csrf(checkout.html) }, 302);
  const orderPath = new URL(placed.location, origin).pathname;
  assert.match(orderPath, /^\/orders\/\d+$/);
  assert.match((await buyer(orderPath)).html, /165\.000 ₫/);
  const other = session();
  await login(other, 'other');
  await other(orderPath, null, 404);
  await buyer('/admin' + orderPath, null, 403);
  result.checks.checkout = { snapshotTotal: 165000, ownerOnly: true };

  step = 'admin pages';
  const admin = session();
  await login(admin, 'admin');
  const orders = await admin('/admin/orders?status=PENDING_CONFIRMATION&method=COD&size=10');
  const detail = await admin('/admin' + orderPath + '?listStatus=PENDING_CONFIRMATION&listMethod=COD&listPage=0&listSize=10');
  const catalog = await admin('/admin/catalog/products');
  const sku = await admin('/admin/catalog/products/1/variants');
  const skuForm = await admin('/admin/catalog/products/1/variants/1/edit');
  for (const [name, page] of Object.entries({ orders, detail, catalog, sku, skuForm })) {
    result.checks[name] = { table: table(page.html), adminFooter: footer(page.html).includes('admin-footer') };
    if (after) {
      labels(page.html);
      assert.ok(result.checks[name].adminFooter);
      assert.equal(result.checks[name].table, name !== 'skuForm');
    }
  }
  assert.ok(detail.html.includes('Địa chỉ thử thứ hai'));
  if (after) {
    assert.match(orders.html, /Số lượng SP/);
    assert.match(orders.html, /Fixture Buyer/);
    assert.match(detail.html, /Xác nhận đơn hàng/);
    assert.ok(detail.html.includes(image));
    assert.match(detail.html, /href="\/admin\/orders\?method=COD&amp;page=0&amp;size=10&amp;status=PENDING_CONFIRMATION#admin-order-/);
    assert.match(catalog.html, /Còn 1 đơn đang xử lý/);
    assert.match(sku.html, /sku-block-1/);
    assert.match(skuForm.html, /sku-status-help/);

    step = 'direct post';
    const path = '/admin/catalog/products/1/variants/1/delete';
    const token = csrf(sku.html);
    await buyer(path, { _csrf: csrf(checkout.html) }, 403);
    await admin(path, {}, 403);
    const denied = await admin(path, { _csrf: token }, 302);
    assert.match((await admin(denied.location)).html, /Không thể ngừng hoạt động vì còn 1 đơn/);
    result.checks.protection = { stalePostDenied: true, customerForbidden: true, csrfRequired: true };
  }
  result.status = 'PASS';
} catch (error) {
  result.status = 'FAIL';
  result.failedStep = step;
  result.failedLine = error.stack?.match(/http-check\.mjs:(\d+)/)?.[1];
  console.error('HTTP QA failed at %s, line %s (%s)', step, result.failedLine, error.name);
  process.exitCode = 1;
} finally {
  await writeFile(output, JSON.stringify(result, null, 2) + '\n');
}
