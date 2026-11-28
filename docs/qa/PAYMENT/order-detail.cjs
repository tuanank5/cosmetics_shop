const assert = require('node:assert/strict');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function check(browser, baseURL, role, width) {
  const context = await browser.newContext({ baseURL, viewport: { width, height: 850 } });
  try {
    await context.route(/^https:\/\//, route => route.abort());
    const page = await context.newPage();
    if (role !== 'guest') {
      await page.goto('/login');
      await page.locator('#username').fill(role + '@example.invalid');
      await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
      await Promise.all([page.waitForNavigation(), page.locator('form[action="/login"] button[type=submit]').click()]);
    }
    const prefix = role === 'admin' ? '/admin/orders/' : '/orders/';
    const response = await page.goto(prefix + '301');
    const history = page.locator('section[aria-labelledby="lich-su-thanh-toan"]');
    if (role === 'guest' || role === 'other') {
      if (role === 'guest') assert.equal(new URL(page.url()).pathname, '/login');
      else assert.equal(response.status(), 404);
      assert.equal(await history.count(), 0);
      return;
    }
    assert.equal(response.status(), 200);
    const rows = history.locator('li');
    assert.equal(await rows.count(), 2);
    assert.match(await rows.first().innerText(), /Lần 1 · MoMo mô phỏng[\s\S]*Thanh toán thất bại/);
    assert.match(await rows.first().innerText(), /Chưa ghi nhận/);
    assert.match(await rows.last().innerText(), /Lần 2 · MoMo mô phỏng[\s\S]*Đã thanh toán/);
    assert.match(await rows.last().innerText(), /28\/11\/2026 03:05:00/);
    assert.match(await rows.last().innerText(), /100[.,\s]000/);
    assert.equal((await page.content()).includes('00000000-0000-4000-8000-000000000002'), role === 'admin');
    assert.equal((await page.content()).includes('78012345'), role === 'admin');
    if (role === 'admin') {
      const confirm = page.locator('form[action="/admin/orders/301/status"] [value="CONFIRMED"]');
      assert.ok(await confirm.isVisible());
    }
    await page.goto(prefix + '302');
    assert.equal(await rows.count(), 1);
    assert.match(await history.innerText(), /Chưa thu tiền/);
    await page.goto(prefix + '303');
    assert.equal(await rows.count(), 0);
    assert.match(await history.innerText(), /chưa có lần thanh toán/);
  } finally {
    await context.close();
  }
}

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  try {
    for (const width of [1280, 390]) {
      for (const role of ['guest', 'other', 'buyer', 'admin']) {
        await check(browser, baseURL, role, width);
        console.log(role + ' ' + width + ' PASS');
      }
    }
  } finally {
    await browser.close();
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
