const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const [baseURL, output] = process.argv.slice(2);
assert.equal(baseURL,'http://127.0.0.1:18516');
(async () => {
  const browser = await webkit.launch();
  const results = [];
  try {
    for (const [id,count] of [[1,1],[3,10],[5,100]]) {
      const context = await browser.newContext({baseURL});
      await context.route(/^https:\/\//, route => route.abort());
      const page = await context.newPage();
      await page.goto('/login');
      await page.locator('#username').fill('perf'+id+'@example.invalid');
      await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
      await Promise.all([page.waitForNavigation(),page.locator('form[action="/login"] button[type=submit]').click()]);
      for (const route of ['/cart','/checkout']) {
        const response = await page.goto(route);
        assert.equal(response.status(),200);
        assert.equal(await page.locator('.cart-count').first().textContent(),String(count));
        const lines = page.locator(route==='/cart'?'[data-cart-line]':'[data-quote-region] .commerce-item');
        assert.equal(await lines.count(),count);
        results.push({route,count,badge:count,status:200});
      }
      assert.equal(await page.locator('[data-quote-region]').getAttribute('data-quote-valid'),'true');
      await context.close();
    }
    fs.writeFileSync(path.join(output,'cart-webkit.json'),JSON.stringify(results,null,2));
  } finally { await browser.close(); }
})().catch(e=>{console.error(e.message);process.exitCode=1;});
