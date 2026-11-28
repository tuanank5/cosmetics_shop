const assert = require('node:assert/strict');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function login(page, user) {
  await page.goto('/login');
  await page.locator('#username').fill(user + '@example.invalid');
  await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
  await Promise.all([page.waitForNavigation(), page.locator('form[action="/login"] button[type=submit]').click()]);
}

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  try {
    for (const [width, user] of [[1280, 'js'], [390, 'ssr']]) {
      const context = await browser.newContext({baseURL, viewport: {width, height:850}});
      const page = await context.newPage();
      await page.goto('/orders');
      assert.equal(new URL(page.url()).pathname, '/login');
      await login(page, user);
      await page.goto('/orders');
      await page.getByRole('searchbox',{name:'Mã đơn'}).fill('BS-');
      await page.locator('[name=status][value=PENDING_CONFIRMATION]').check();
      await page.locator('[name=sort]').selectOption('totalAsc');
      await page.locator('[name=fromDate]').fill('2020-01-01');
      await page.locator('[name=toDate]').fill('2099-01-01');
      await Promise.all([page.waitForNavigation(), page.getByRole('button',{name:'Áp dụng'}).click()]);
      const rows = page.locator('tr[data-scroll-row]');
      assert.equal(await rows.count(), 1);
      const link = rows.getByRole('link');
      await Promise.all([page.waitForNavigation(),link.click()]);
      const detail = page.url();
      await Promise.all([page.waitForNavigation(),page.locator('nav a[href*="#customer-order-"]').click()]);
      const back = new URL(page.url());
      assert.equal(back.searchParams.get('status'),'PENDING_CONFIRMATION');
      assert.equal(back.searchParams.get('sort'),'totalAsc');
      assert.equal(back.searchParams.get('q'),'BS-');
      assert.equal(back.searchParams.get('fromDate'),'2020-01-01');
      assert.equal(back.searchParams.get('toDate'),'2099-01-01');
      assert.match(back.hash,/^#customer-order-/);
      await page.waitForFunction(() => document.activeElement.id === location.hash.slice(1));
      await page.goto(detail);
      await page.reload();
      const other = await browser.newContext({baseURL});
      const stranger = await other.newPage();
      await login(stranger,'other');
      assert.equal((await stranger.goto(detail)).status(),404);
      await other.close();
      await Promise.all([page.waitForURL('**/cart'),page.getByRole('button',{name:'Mua lại'}).dblclick()]);
      const result = await page.locator('[aria-label="Kết quả mua lại"]').innerText();
      assert.match(result,/Đã thêm đủ 1/);
      assert.match(result,/Bỏ qua vì biến thể không còn bán/);
      await page.goto('/checkout');
      assert.match(await page.locator('[data-checkout-total]').innerText(),/225.000/);
      assert.equal(await page.locator('#voucher').inputValue(),'');
      assert.equal(await page.locator('#ship').inputValue(),'');
      await Promise.all([page.waitForURL('**/orders/*'), page.locator('[data-place-order]').click()]);
      assert.notEqual(new URL(page.url()).pathname, new URL(detail).pathname);
      assert.match(await page.locator('main').innerText(), /225.000/);
      await context.close();
      console.log('PASS ' + width + ' filters, Back, ownership, mixed reorder, double click and fresh checkout');
    }
  } finally { await browser.close(); }
}
main().catch(error => { console.error(error.message); process.exitCode=1; });
