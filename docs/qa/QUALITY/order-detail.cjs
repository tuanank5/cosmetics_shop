const assert = require('node:assert/strict');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  try {
    for (const role of ['buyer', 'other', 'admin']) {
      const context = await browser.newContext({ baseURL });
      await context.route(/^https:\/\//, route => route.abort());
      const page = await context.newPage();
      await page.goto('/login');
      await page.locator('#username').fill(role + '@example.invalid');
      await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
      await Promise.all([page.waitForNavigation(), page.locator('form[action="/login"] button[type=submit]').click()]);
      const path = role === 'admin' ? '/admin/orders/301' : '/orders/301';
      const response = await page.goto(path);
      assert.equal(response.status(), role === 'other' ? 404 : 200);
      if (role === 'admin') {
        assert.ok(await page.locator('[data-status="PAID"]').first().isVisible());
        assert.equal(await page.locator('form[action="/admin/orders/301/status"] [value="CONFIRMED"]').count(), 1);
      } else if (role === 'buyer') {
        assert.ok((await page.locator('main').innerText()).includes('QUALITY-301'));
      }
      console.log(role + ' order detail PASS');
      await context.close();
    }
  } finally {
    await browser.close();
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
