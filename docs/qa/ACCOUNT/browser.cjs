const assert = require('node:assert/strict');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  let step = 'login';
  try {
    async function page() {
      const ctx = await browser.newContext({ baseURL });
      await ctx.route('**/*', route => new URL(route.request().url()).hostname === '127.0.0.1'
        ? route.continue() : route.abort());
      return ctx.newPage();
    }
    async function login(p, method, user) {
      if (method !== 'password') return p.goto('/oauth2/authorization/' + method);
      await p.goto('/login');
      await p.locator('#username').fill(user);
      await p.locator('#password').fill(process.env.ACCOUNT_FIXTURE_PASSWORD);
      await Promise.all([p.waitForNavigation(), p.locator('form[action="/login"] button[type=submit]').click()]);
    }
    const admin = await page();
    const customer = process.env.ACCOUNT_FIXTURE_CUSTOMER;
    await login(admin, 'password', process.env.ACCOUNT_FIXTURE_ADMIN);
    const methods = ['password', 'google', 'github'];
    const sessions = [];
    for (const method of methods) {
      const p = await page();
      await login(p, method, customer);
      await p.goto('/account');
      assert.equal(new URL(p.url()).pathname, '/account');
      sessions.push(p);
    }
    async function change(reason) {
      await admin.goto('/admin/users?q=' + encodeURIComponent(customer));
      const form = admin.locator('form[action$="/status"]');
      await form.locator('select').selectOption(reason);
      await Promise.all([admin.waitForNavigation(), form.evaluate(f => f.requestSubmit())]);
      assert.equal(new URL(admin.url()).searchParams.get('q'), customer);
      assert.ok((await admin.locator('main').innerText()).includes('Chờ gửi'));
    }
    step = 'lock';
    await change('BAO_VE_TAI_KHOAN');
    for (const method of methods) {
      step = 'deny ' + method;
      const p = await page();
      await login(p, method, customer);
      await p.goto('/account');
      assert.equal(new URL(p.url()).pathname, '/login');
    }
    step = 'unlock and revoked sessions';
    await sessions[0].goto('/account');
    assert.equal(new URL(sessions[0].url()).pathname, '/login');
    await change('DA_XU_LY_RUI_RO');
    for (const [index, method] of methods.entries()) {
      const p = sessions[index];
      await p.goto('/account');
      assert.equal(new URL(p.url()).pathname, '/login');
      await login(p, method, customer);
      await p.goto('/account');
      assert.equal(new URL(p.url()).pathname, '/account');
    }
    console.log('[ACCOUNT WebKit] PASS password, Google OIDC fixture, GitHub fixture, stale sessions and fresh login');
  } catch (err) {
    console.error('ACCOUNT WebKit failed at ' + step + ' (' + err.name + ')');
    process.exitCode = 1;
  } finally { await browser.close(); }
}
main().catch(() => { process.exitCode = 1; });
