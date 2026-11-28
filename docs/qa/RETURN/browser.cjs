const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { webkit } = require(process.env.BASE_PLAYWRIGHT);
const [baseURL, order] = process.argv.slice(2);
assert.equal(new URL(baseURL).hostname, '127.0.0.1');
let step = 'launch';

(async () => {
  const browser = await webkit.launch();
  try {
    async function login(user, viewport, javaScriptEnabled) {
      const context = await browser.newContext({baseURL, viewport, javaScriptEnabled});
      await context.route('**/*', route => new URL(route.request().url()).origin === baseURL
        ? route.continue() : route.abort());
      const page = await context.newPage();
      page.setDefaultTimeout(10000);
      await page.goto('/login');
      await page.locator('#username').fill(user);
      await page.locator('#password').fill(process.env.RETURN_PASS);
      await Promise.all([page.waitForNavigation(), page.locator('form[action="/login"] button[type="submit"]').click()]);
      return page;
    }
    const admin = await login(process.env.RETURN_ADMIN, {width: 1280, height: 800}, true);
    const customer = await login(process.env.RETURN_CUSTOMER, {width: 390, height: 844}, false);
    step = 'customer request';
    const opened = await customer.goto('/orders/' + order + '/returns/new');
    assert.equal(opened.status(), 200);
    const quantity = customer.locator('input[name^="soLuong"]');
    step = 'customer quantity ' + new URL(customer.url()).pathname + ' fields=' + await quantity.count()
      + ' visible=' + await quantity.first().isVisible();
    await quantity.fill('3');
    step = 'customer reason';
    await customer.locator('[name=lyDo]').fill('Kiểm tra hai đợt nhận hàng');
    step = 'customer submit';
    await Promise.all([customer.waitForNavigation(), customer.locator('form.catalog-form button[type=submit]').click()]);
    const path = new URL(customer.url()).pathname;
    assert.match(path, /^\/account\/returns\/\d+$/);
    const id = path.split('/').pop();
    const submit = async (action, button) => {
      step = action;
      const form = admin.locator('form[action$="/' + action + '"]');
      await Promise.all([admin.waitForNavigation(), form.locator(button || 'button').click()]);
      assert.ok((await admin.locator('main').innerText()).includes('Đã ghi nhận thao tác'));
    };
    await admin.goto('/admin/returns/' + id);
    await submit('status','button[value="APPROVED"]');
    const parcel = randomUUID().toUpperCase();
    await admin.locator('[name=thamChieu]').fill(parcel);
    await admin.locator('form[action$="/receive"] input[name^="soLuong"]').fill('1');
    await submit('receive');
    await admin.locator('[name=daMo]').fill('1');
    await submit('inspect');
    await submit('confirm');
    step = 'customer partial';
    await customer.goto('/account/returns/' + id);
    const detail = await customer.locator('main').innerText();
    assert.ok(detail.includes('Cách ly 1') && detail.includes('Chưa nhận 2') && detail.includes('Đã mở 1'));
    assert.ok(!detail.includes(parcel));
    await admin.locator('[name=thamChieu]').fill(randomUUID());
    await admin.locator('form[action$="/receive"] input[name^="soLuong"]').fill('1');
    await submit('receive');
    await admin.locator('[name=duBan]').fill('1');
    await admin.locator('[name=datChecklist]').check();
    await submit('inspect');
    await submit('confirm');
    await admin.reload();
    await customer.reload();
    const second = await customer.locator('main').innerText();
    assert.ok(second.includes('Đã nhận 2 · Đủ bán 1 · Cách ly 1') && second.includes('Chưa nhận 1'));
    await admin.locator('[name=nghiaVu]').fill('FIXTURE-SETTLED');
    await admin.locator('[name=thieu]').fill('FIXTURE-SHORT');
    await submit('close');
    await customer.reload();
    assert.ok((await customer.locator('main').innerText()).includes('Đã hoàn tất'));
    console.log('[RETURN WebKit] PASS');
  } finally { await browser.close(); }
})().catch(error => {
  console.error('RETURN WebKit failed at ' + step + ' (' + error.name + ')');
  process.exitCode = 1;
});
