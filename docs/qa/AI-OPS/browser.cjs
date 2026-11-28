const { webkit } = require(process.env.BASE_PLAYWRIGHT);
const assert = require('node:assert/strict');
const [origin, mode] = process.argv.slice(2);
assert.equal(new URL(origin).hostname, '127.0.0.1');
let step = 'launch';
(async () => {
  const browser = await webkit.launch();
  try {
    const ctx = await browser.newContext({ baseURL: origin, javaScriptEnabled: false,
      viewport: mode === 'disabled' ? {width:390,height:844} : {width:1280,height:800} });
    await ctx.route('**/*', route => new URL(route.request().url()).origin === origin ? route.continue() : route.abort());
    const page = await ctx.newPage();
    page.setDefaultTimeout(10000);
    step = 'guest FAQ';
    await page.goto('/chat');
    if (mode === 'disabled') {
      await page.locator('#cauHoi').fill('Đổi trả trong bao lâu?');
      await Promise.all([page.waitForNavigation(), page.locator('form[action="/chat"] button').click()]);
      assert.match(await page.locator('main').innerText(), /7 ngày/);
    }
    step = 'login';
    await page.goto('/admin/ai');
    assert.equal(new URL(page.url()).pathname, '/login');
    await page.locator('#username').fill(process.env.FIXTURE_USER);
    await page.locator('#password').fill(process.env.FIXTURE_PASS);
    await Promise.all([page.waitForNavigation(),page.locator('form[action="/login"] button[type=submit]').click()]);
    step = 'admin';
    await page.goto('/admin/ai');
    let body = await page.locator('main').innerText();
    assert.match(body, /Embedding truy vấn/);
    if (mode === 'disabled') {
      assert.match(body, /Tư vấn AI đang tắt/);
      assert.match(body, /chưa kiểm tra/);
      assert.equal(await page.locator('form[action$="reindex"]').count(), 0);
    } else {
      step = 'reindex';
      await Promise.all([page.waitForNavigation(), page.locator('form[action$="reindex"] button').click()]);
      body = await page.locator('main').innerText();
      assert.match(body, /Đã làm mới nguồn/);
      assert.match(body, /Phản hồi HTTP gần nhất/);
    }
    step = 'retired route';
    const old = await page.goto('/tu-van-da');
    assert.equal(old.status(),404);
    console.log('[AI-OPS WebKit] PASS '+mode);
  } finally { await browser.close(); }
})().catch(e => { console.error('AI-OPS WebKit failed at '+step+' ('+e.name+')'); process.exitCode=1; });
