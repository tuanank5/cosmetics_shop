const assert = require('node:assert/strict');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');

async function main() {
  const baseURL = process.argv[2];
  assert.equal(new URL(baseURL).hostname, '127.0.0.1');
  const browser = await webkit.launch();
  let step = 'reset';
  try {
    const context = await browser.newContext({ baseURL });
    await context.route(/^https:\/\//, route => route.abort());
    const page = await context.newPage();
    page.setDefaultTimeout(8000);
    const form = path => page.locator('form[action="' + path + '"]');
    const submit = async path => {
      await Promise.all([
        page.waitForNavigation({ waitUntil: 'domcontentloaded' }),
        form(path).evaluate(element => element.requestSubmit())
      ]);
    };
    const selectPassword = async (value, path) => {
      await page.locator('#matKhauMoi').fill(value);
      await page.locator('#xacNhan').fill(value);
      await submit(path);
    };
    await page.goto('/login');
    const sessionId = async () => (await context.cookies(baseURL)).find(cookie => cookie.name === 'JSESSIONID')?.value;
    const beforeReset = await sessionId();
    await page.goto('/reset-password?token=' + process.env.AUTH_RESET_TOKEN);
    const duringReset = await sessionId();
    assert.ok(beforeReset && duringReset && beforeReset !== duringReset);
    await selectPassword(process.env.AUTH_OLD_PASSWORD, '/reset-password');
    assert.ok((await page.locator('[role=alert]').innerText()).includes('khác mật khẩu hiện tại'));
    await selectPassword(process.env.AUTH_NEW_PASSWORD, '/reset-password');
    assert.equal(new URL(page.url()).pathname, '/login');
    await page.goto('/reset-password?token=' + process.env.AUTH_RESET_TOKEN);
    assert.equal(await page.locator('#matKhauMoi').count(), 0);

    step = 'google-login';
    const google = page.locator('a[href="/oauth2/authorization/google"]');
    await page.goto('/login');
    const beforeLogin = await sessionId();
    await google.click();
    const afterLogin = await sessionId();
    assert.ok(beforeLogin && afterLogin && beforeLogin !== afterLogin);
    await page.goto('/account');
    assert.equal(new URL(page.url()).pathname, '/account');
    const second = await context.newPage();
    await second.goto('/orders');
    assert.equal(new URL(second.url()).pathname, '/orders');
    step = 'logout';
    await submit('/logout');
    assert.equal(new URL(page.url()).search, '?logout');
    await second.reload();
    assert.equal(new URL(second.url()).pathname, '/login');
    await page.goBack();
    await page.reload();
    assert.notEqual(new URL(page.url()).pathname, '/account');
    for (const path of ['/account', '/orders']) {
      await page.goto(path);
      assert.equal(new URL(page.url()).pathname, '/login');
    }
    step = 'new-callback';
    await google.click();
    await page.goto('/account/security');
    assert.equal(new URL(page.url()).pathname, '/account/security');
    assert.equal(await page.locator('#matKhauMoi').count(), 0);
    step = 'oauth-reauth';
    await submit('/account/security/oauth/google');
    await submit('/account/security/email/send');
    await page.locator('#ma').fill('123456');
    await submit('/account/security/email/verify');
    step = 'last-method';
    await submit('/account/security/oauth/google/unlink');
    assert.ok((await page.locator('body').innerText()).includes('phương thức sẽ giữ'));
    step = 'first-password';
    await selectPassword(process.env.AUTH_NEW_PASSWORD, '/account/security/password');
    assert.equal(new URL(page.url()).pathname, '/login');
    console.log('[AUTH WebKit] PASS ' + browser.version() + ' reset/retry/replay, two-tab logout, new OIDC callback, reauth/OTP, last method, first password');
  } catch {
    throw Error('Browser check failed at ' + step);
  } finally {
    await browser.close();
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
