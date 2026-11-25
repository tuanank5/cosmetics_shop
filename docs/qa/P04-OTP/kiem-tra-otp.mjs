import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { setTimeout as delay } from 'node:timers/promises';

const app = process.env.P04_QA_APP;
const smtp = process.env.P04_QA_MAILPIT;
const password = randomUUID();
const email = () => `${randomUUID()}@example.invalid`;

function redis(...args) {
  return execFileSync(process.env.P04_QA_REDIS_CLI, ['-p', process.env.OTP_REDIS_PORT, ...args], {
    encoding: 'utf8',
  }).trim();
}

async function shortenExpiry(pattern) {
  const keys = redis('--scan', '--pattern', pattern).split(/\r?\n/);
  assert.equal(keys.length, 1);
  assert.ok(Number(redis('PTTL', keys[0])) > 0);
  assert.equal(redis('PEXPIRE', keys[0], '1'), '1');
  await delay(25);
}

class Browser {
  cookies = new Map();

  async request(path, data) {
    const response = await fetch(app + path, {
      method: data ? 'POST' : 'GET',
      body: data ? new URLSearchParams(data) : undefined,
      headers: { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; ') },
      redirect: 'manual',
    });
    for (const cookie of response.headers.getSetCookie()) {
      const [key, value] = cookie.split(';')[0].split('=');
      this.cookies.set(key, value);
    }
    return { status: response.status, location: response.headers.get('location'), html: await response.text() };
  }

  async post(path, data, page = '/register') {
    const form = await this.request(page);
    assert.equal(form.status, 200);
    const token = form.html.match(/name="_csrf" value="([^"]+)"/)?.[1];
    assert.ok(token, 'Form có CSRF token');
    return this.request(path, { ...data, _csrf: token });
  }
}

async function latestCode(recipient) {
  const { messages } = await (await fetch(smtp + '/api/v1/messages')).json();
  const message = messages.find(({ To }) => To.some(({ Address }) => Address === recipient));
  assert.ok(message, 'Mailpit nhận được thư đăng ký');
  const { Text } = await (await fetch(smtp + '/api/v1/message/' + message.ID)).json();
  const code = Text.match(/\b\d{6}\b/)?.[0];
  assert.ok(code, 'Thư có mã 6 chữ số');
  return { code, messageId: message.ID };
}

function redirected(result, path) {
  assert.equal(result.status, 302);
  assert.equal(new URL(result.location, app).pathname + new URL(result.location, app).search, path);
}

const first = new Browser();
const second = new Browser();
const address = email();
const registration = { email: address, matKhau: password, hoTen: 'Khách kiểm thử' };
assert.equal((await first.request('/register', registration)).status, 403);
redirected(await first.post('/register', registration), '/register');
const firstCode = await latestCode(address);
assert.equal((await first.request('/register/verify-otp', { otp: firstCode.code })).status, 403);
redirected(await first.post('/login', { username: address, password }, '/login'), '/login?error');

const cooldown = await second.post('/register', registration);
assert.equal(cooldown.status, 200);
assert.ok(cooldown.html.includes('60 giây'));
assert.equal((await latestCode(address)).messageId, firstCode.messageId);
await shortenExpiry('beautyshop:register:send:*');
redirected(await second.post('/register', registration), '/register');
const secondCode = await latestCode(address);
assert.notEqual(secondCode.messageId, firstCode.messageId);
assert.notEqual(secondCode.code, firstCode.code, 'Test cần hai mã ngẫu nhiên khác nhau');
redirected(await second.post('/register/verify-otp', { otp: firstCode.code }), '/register');
redirected(await first.post('/register/verify-otp', { otp: firstCode.code }), '/login?registered');
redirected(await first.post('/register/verify-otp', { otp: firstCode.code }), '/register');
redirected(await second.post('/register/verify-otp', { otp: secondCode.code }), '/register');
redirected(await first.post('/login', { username: address, password }, '/login'), '/');
assert.equal((await first.request('/account')).status, 200);
console.log('PASS: SMTP, giới hạn theo email, cô lập giữa hai phiên, dùng một lần, email UNIQUE, đăng nhập sau OTP và CSRF');

const third = new Browser();
const expiredAddress = email();
redirected(await third.post('/register', { ...registration, email: expiredAddress }), '/register');
const expiredCode = await latestCode(expiredAddress);
await shortenExpiry('beautyshop:register:code:*');
redirected(await third.post('/register/verify-otp', { otp: expiredCode.code }), '/register');
assert.ok((await third.request('/register')).html.includes('đã hết hạn'));
const sendKeys = redis('--scan', '--pattern', 'beautyshop:register:send:*').split(/\r?\n/);
for (const key of sendKeys) {
  redis('PEXPIRE', key, '1');
}
await delay(25);
redirected(await third.post('/register/resend-otp', {}), '/register');
const newCode = await latestCode(expiredAddress);
assert.notEqual(newCode.messageId, expiredCode.messageId);
const wrong = newCode.code === '000000' ? '111111' : '000000';
for (let attempt = 0; attempt < 5; attempt++) {
  redirected(await third.post('/register/verify-otp', { otp: wrong }), '/register');
}
redirected(await third.post('/register/verify-otp', { otp: newCode.code }), '/register');
redirected(await third.post('/register/cancel', {}), '/register');
assert.ok(!(await third.request('/register')).html.includes('name="otp"'));
console.log('PASS: Redis TTL, gửi lại OTP, khóa mã sau 5 lần sai và hủy đăng ký');
