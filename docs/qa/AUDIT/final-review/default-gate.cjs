const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  try {
    for (const role of ['buyer','admin']) {
      const ctx = await browser.newContext({baseURL});
      const page = await ctx.newPage();
      await page.goto('/login');
      await page.locator('#username').fill(role + '@example.invalid');
      await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
      await Promise.all([page.waitForNavigation(), page.locator('form[action="/login"] button[type=submit]').click()]);
      await page.goto(role === 'admin' ? '/admin/audit?q=FIXTURE_READ' : '/account');
      const csrf = await page.locator('input[name="_csrf"]').first().inputValue();
      const response = await ctx.request.post('/admin/operations/audit/retention',{form:{
        correlation:randomUUID(),dryRun:'false',actor:'3',clearanceSourceEnabled:'true',_csrf:csrf
      }});
      assert.equal(response.status(),role === 'admin' ? 409 : 403);
      if (role === 'admin') {
        await page.reload();
        assert.match(await page.locator('[data-scroll-list-start]').innerText(), /26/);
      }
      console.log(role + ' default retention gate PASS');
      await ctx.close();
    }
  } finally { await browser.close(); }
}
main().catch(e => {console.error(e.message); process.exitCode=1;});
