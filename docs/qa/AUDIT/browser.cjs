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
    const ctx = await browser.newContext({ baseURL });
    const page = await ctx.newPage();
    await page.goto('/admin/audit');
    assert.equal(new URL(page.url()).pathname, '/login');
    await login(page, 'buyer');
    for (const path of ['/admin/audit', '/admin/audit/1']) {
      assert.equal((await page.goto(path)).status(), 403);
      assert.ok(!(await page.content()).includes('FIXTURE_READ'));
    }
    await ctx.close();
    console.log('Guest and Customer permissions PASS');
    for (const javaScriptEnabled of [false,true]) {
      const ctx = await browser.newContext({ baseURL, javaScriptEnabled });
      const page = await ctx.newPage();
      await login(page, 'admin');
      const path = '/admin/audit?q=FIXTURE_READ&actor=3&type=SKU&result=SUCCESS&date=2000-01-01&sort=oldest&page=1';
      assert.equal((await page.goto(path)).status(), 200);
      const rows = page.locator('[data-scroll-row]');
      assert.equal(await rows.count(), 6);
      const link = rows.first().locator('a');
      const id = await link.textContent();
      const tab = await ctx.newPage();
      await tab.goto('/admin/audit?result=FAILED');
      await link.click();
      assert.ok(await page.getByRole('heading', { name: 'Sự kiện #' + id }).isVisible());
      assert.ok((await page.locator('main').innerText()).includes('Chưa ghi nhận'));
      await page.getByRole('link', {name:'Quay lại nhật ký'}).click();
      assert.equal(new URL(page.url()).searchParams.get('q'), 'FIXTURE_READ');
      assert.equal(new URL(page.url()).searchParams.get('page'), '1');
      assert.equal(new URL(page.url()).hash, '#audit-' + id);
      assert.equal(new URL(tab.url()).searchParams.get('result'), 'FAILED');
      if (javaScriptEnabled) {
        await page.waitForFunction(id => document.activeElement.id === 'audit-' + id, id);
        const visible = await page.locator('#audit-' + id).boundingBox();
        assert.ok(visible.y < page.viewportSize().height && visible.y + visible.height > 0);
        await link.click();
        await page.goBack();
        await page.waitForFunction(id => document.activeElement.id === 'audit-' + id, id);
        await link.click();
        const csrf = await page.locator('input[name="_csrf"]').first().inputValue();
        const url = '/admin/operations/audit/retention';
        const form = { correlation:process.env.AUDIT_FIXTURE_BATCH, dryRun:'false', batch:'26' };
        assert.equal((await ctx.request.post(url, {form})).status(), 403);
        const purge = await ctx.request.post(url,{form:{...form,_csrf:csrf}});
        assert.equal(purge.status(), 200);
        assert.equal((await purge.json()).daDon, 26);
        await page.getByRole('link', {name:'Quay lại nhật ký'}).click();
        await page.waitForFunction(() => document.activeElement.hasAttribute('data-scroll-list-start'));
        assert.equal(await rows.count(), 0);
      }
      await ctx.close();
      console.log('Admin list/detail/back two tabs JS=' + javaScriptEnabled + ' PASS');
    }
  } finally { await browser.close(); }
}
main().catch(e => {console.error(e.message); process.exitCode=1;});
