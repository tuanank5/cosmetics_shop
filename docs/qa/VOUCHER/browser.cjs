const assert = require('node:assert/strict');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function login(page, role) {
  await page.goto('/login');
  await page.locator('#username').fill(role + '@example.invalid');
  await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
  await Promise.all([page.waitForNavigation(), page.locator('form[action="/login"] button[type=submit]').click()]);
}

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  try {
    for (const javaScriptEnabled of [false, true]) {
      const ctx = await browser.newContext({baseURL,javaScriptEnabled});
      const page = await ctx.newPage();
      assert.equal(new URL((await page.goto('/checkout')).url()).pathname,'/login');
      await login(page,javaScriptEnabled ? 'js' : 'ssr');
      assert.equal((await page.goto('/admin/vouchers')).status(),403);
      await page.goto('/checkout');
      await page.locator('#ship').fill('S10');
      const quote = async code => {
        await page.locator('#voucher').fill(code);
        if (!javaScriptEnabled) await page.locator('[data-checkout-form] button[type=submit]').click();
      };
      await quote('H30');
      await page.waitForFunction(() => document.querySelector('[data-checkout-total]')?.textContent.includes('505.000'));
      assert.equal(await page.locator('#checkout-order input[name=ship]').inputValue(),'S10');
      await page.locator('#ship').fill('H30');
      await quote('');
      await page.waitForFunction(() => document.querySelector('[data-quote-confirmed]')?.textContent.includes('miễn phí'));
      await quote('H30');
      await page.waitForFunction(() => document.querySelector('.error-summary')?.textContent.includes('một ô'));
      await page.locator('#ship').fill('S10');
      await quote('EXPIRED');
      await page.waitForFunction(() => document.querySelector('#voucher-error')?.textContent.includes('hết hạn'));
      await quote('');
      await page.waitForFunction(() => document.querySelector('[data-quote-confirmed]')?.textContent.includes('miễn phí'));
      assert.match(await page.locator('[data-checkout-total]').innerText(),/520.000/);
      await quote('H30');
      await page.waitForFunction(() => document.querySelector('[data-checkout-total]')?.textContent.includes('505.000'));
      await Promise.all([page.waitForURL("**/orders/*"), page.locator('[data-place-order]').click()]);
      assert.match(await page.locator('main').innerText(),/505.000/);
      await ctx.close();
      console.log('Customer two slots, expiry, free shipping, role JS='+javaScriptEnabled+' PASS');
    }
    const ctx = await browser.newContext({baseURL,javaScriptEnabled:false});
    const page = await ctx.newPage();
    await page.goto('/products/fixture-a');
    await Promise.all([page.waitForURL("**/cart"), page.locator('form[action="/cart/items"] button[type=submit]').first().click()]);
    await page.goto('/cart?hang=H30&ship=S10');
    assert.match(await page.locator('.cart-summary').innerText(),/355.000/);
    await login(page,'buyer');
    await page.goto('/cart');
    assert.match(await page.locator('.cart-summary').innerText(),/890.000/);
    await page.goto('/checkout');
    await page.locator('#ship').fill('S10');
    await page.locator('#voucher').fill('H30');
    await page.locator('[data-checkout-form] button[type=submit]').click();
    assert.match(await page.locator('[data-quote-confirmed]').innerText(),/miễn phí/);
    await Promise.all([page.waitForURL('**/orders/*'),page.locator('[data-place-order]').click()]);
    assert.match(await page.locator('main').innerText(),/860.000/);
    await ctx.close();
    console.log('Guest estimate and cart merge PASS');
    const admin = await browser.newContext({baseURL});
    const view = await admin.newPage();
    await login(view,'admin');
    await view.goto('/admin/vouchers/new');
    await view.locator('#maApDung').fill('WEB-SHIP');
    await view.locator('#slot').selectOption('VAN_CHUYEN');
    await view.locator('#loaiGiamGia').selectOption('FIXED');
    for (const [id,value] of Object.entries({giaTriGiam:'1000',gioiHanSuDung:'4',gioiHanKhach:'1',sanPham:'1',
        batDauLuc:'2020-01-01T00:00',ketThucLuc:'2099-01-01T00:00'})) await view.locator('#'+id).fill(value);
    await Promise.all([view.waitForURL('**/admin/vouchers'),view.locator('.catalog-form button[type=submit]').click()]);
    assert.match(await view.locator('main').innerText(),/WEB-SHIP/);
    await admin.close();
    console.log('Admin create scoped shipping voucher PASS');
  } finally { await browser.close(); }
}
main().catch(error => {console.error(error.message); process.exitCode=1;});
