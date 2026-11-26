import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const [base, scratch, mode, mongoPid] = process.argv.slice(2);
assert.equal(new URL(base).hostname, '127.0.0.1');
const profile = path.join(scratch, 'chrome');
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless', '--remote-debugging-port=0', '--no-first-run', '--disable-extensions',
  '--disable-background-networking', '--force-prefers-reduced-motion', '--user-data-dir=' + profile, 'about:blank',
], { windowsHide: true, stdio: 'ignore' });
const checks = [];
const trace = {};
let socket;

async function until(check) {
  const end = Date.now() + 12000;
  for (;;) {
    try { return await check(); }
    catch (error) {
      if (Date.now() >= end) throw error;
      await new Promise(resolve => setTimeout(resolve, 120));
    }
  }
}
try {
  const port = await until(async () => (await readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]);
  const endpoint = 'http://127.0.0.1:' + port;
  const { Browser } = await (await fetch(endpoint + '/json/version')).json();
  const [tab] = (await (await fetch(endpoint + '/json/list')).json()).filter(target => target.type === 'page');
  socket = new WebSocket(tab.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', reject, { once: true });
  });
  let sequence = 0;
  const requests = new Map();
  socket.addEventListener('message', event => {
    const { id, result, error } = JSON.parse(event.data);
    requests.get(id)?.(error, result);
  });
  function call(method, params = {}, sessionId) {
    const id = ++sequence;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        requests.delete(id);
        reject(new Error(method + ' timed out'));
      }, 12000);
      requests.set(id, (error, result) => {
        clearTimeout(timer);
        requests.delete(id);
        if (error) reject(new Error(method + ': ' + error.message));
        else resolve(result);
      });
      socket.send(JSON.stringify({ id, method, params, sessionId }));
    });
  }
  async function evaluate(expression, label = 'Browser evaluation', sessionId) {
    const result = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }, sessionId);
    assert.ok(!result.exceptionDetails, label);
    return result.result.value;
  }
  async function go(route, sessionId) {
    const url = base + route;
    const response = await call('Page.navigate', { url }, sessionId);
    assert.ok(!response.errorText, response.errorText);
    await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState', 'Wait for navigation', sessionId), url + '|complete'));
  }
  async function key(key, windowsVirtualKeyCode, code = key) {
    const params = { key, code, windowsVirtualKeyCode };
    const text = key === 'Enter' ? '\r' : undefined;
    await call('Input.dispatchKeyEvent', { type: 'keyDown', ...params, text });
    await call('Input.dispatchKeyEvent', { type: 'keyUp', ...params });
  }
  function viewport(width, height, dpr = 1) {
    return call('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: dpr, mobile: false });
  }
  async function capture(name) {
    const directory = path.resolve('docs/ui/screenshots/REVIEW-01');
    await mkdir(directory, { recursive: true });
    const result = await call('Page.captureScreenshot', { format: 'webp', quality: 78 });
    await writeFile(path.join(directory, name + '.webp'), Buffer.from(result.data, 'base64'));
  }

  await call('Page.enable');
  await call('Network.enable');
  await call('Network.setBlockedURLs', {
    urls: ['https://*', 'http://*'],
    urlPatterns: [{ urlPattern: base + '/*', block: false }],
  });
  await viewport(390, 850);
  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/login');
  await evaluate('document.querySelector("#username").focus()');
  await call('Input.insertText', { text: 'admin@example.invalid' });
  await evaluate('document.querySelector("#password").focus()');
  await call('Input.insertText', { text: process.env.REVIEW01_FIXTURE_PASS });
  await key('Enter', 13);
  await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/admin/dashboard|complete'));

  const filter = '/admin/reviews?status=';
  const row = '.admin-review-list li:first-child';
  const button = row + ' button';
  const all = filter + 'TAT_CA&stars=4&product=1&page=0';
  const pending = filter + 'CHO_DUYET&stars=4&product=1&page=1';
  const approved = filter + 'DANG_HIEN&stars=4&product=1&page=0';
  const snapshot = () => evaluate(`(() => {
    const form = document.querySelector('.review-filter');
    const row = document.querySelector(${JSON.stringify(row)});
    const rect = node => {
      if (!node) return null;
      const r = node.getBoundingClientRect();
      return { y: Math.round(r.top + scrollY), viewportY: Math.round(r.top), width: Math.round(r.width), height: Math.round(r.height) };
    };
    const button = row?.querySelector('button');
    const focus = document.activeElement;
    return { filter: rect(form), row: rect(row), button: rect(button), badge: rect(row?.querySelector('.badge')),
      state: row?.querySelector('.badge')?.textContent.trim() ?? null,
      count: document.querySelector('#review-results,.admin-reviews > p.muted')?.textContent.trim() ?? null,
      alert: document.querySelector('[data-page-notices]')?.textContent.trim() ?? '',
      scroll: Math.round(scrollY), focus: focus?.id || (focus === button ? 'row-button' : focus?.tagName),
      loading: button?.classList.contains('is-loading') ?? false,
      page: new URLSearchParams(location.search).get('page') };
  })()`);
  const focusButton = () => evaluate(`document.querySelector(${JSON.stringify(button)}).focus()`);
  const flash = phrase => until(async () => assert.ok(await evaluate(
    `document.readyState === 'complete' && document.querySelector('[data-page-notices]')?.textContent.includes(${JSON.stringify(phrase)})`)));
  const beginThrottle = () => call('Network.emulateNetworkConditions', {
    offline: false, latency: 650, downloadThroughput: -1, uploadThroughput: -1,
  });
  const stopThrottle = () => call('Network.emulateNetworkConditions', {
    offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1,
  });
  const submit = async (name, phrase, measureLoading = true) => {
    await focusButton();
    trace[name] = { before: await snapshot() };
    if (measureLoading) await beginThrottle();
    await key('Enter', 13);
    if (measureLoading) trace[name].loading = await snapshot();
    await flash(phrase);
    if (measureLoading) await stopThrottle();
    trace[name].after = await snapshot();
  };

  await call('Emulation.setScriptExecutionDisabled', { value: false });
  await go(all);
  await submit('approveAll', 'Đã duyệt đánh giá.');
  await go(approved);
  await submit('hideVisible', 'Đã ẩn đánh giá.');
  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go(pending);
  await submit('approveLastPendingNoJs', 'Đã duyệt đánh giá.', false);
  const evidence = 'docs/qa/REVIEW-01/' + (mode === 'baseline' ? 'browser-baseline.json' : 'browser-results.json');
  if (mode === 'baseline') {
    await writeFile(evidence, JSON.stringify({ browser: Browser, trace }, null, 2) + '\n');
  } else {
    assert.equal(trace.approveAll.before.filter.y, trace.approveAll.after.filter.y);
    assert.equal(trace.approveAll.before.row.y, trace.approveAll.after.row.y);
    assert.equal(trace.approveAll.before.button.width, trace.approveAll.loading.button.width);
    assert.equal(trace.approveAll.before.button.width, trace.approveAll.after.button.width);
    assert.equal(trace.approveAll.before.badge.width, trace.approveAll.after.badge.width);
    assert.ok(trace.approveAll.loading.loading);
    assert.equal(trace.approveAll.after.focus, 'row-button');
    assert.equal(trace.approveAll.after.state, 'Đang hiện');
    assert.equal(trace.hideVisible.after.row, null);
    assert.equal(trace.hideVisible.after.focus, 'review-results');
    assert.equal(trace.hideVisible.after.count, '0 đánh giá');
    checks.push('retained-row-stable-and-focus-restored-removed-row-focuses-heading');
    assert.equal(trace.approveLastPendingNoJs.after.page, '0');
    assert.equal(trace.approveLastPendingNoJs.after.count, '20 đánh giá');
    assert.equal(trace.approveLastPendingNoJs.after.focus, 'review-results');
    assert.ok(await evaluate('document.documentElement.scrollWidth <= innerWidth'));
    await capture('mobile-clamped');
    checks.push('keyboard-no-js-last-page-clamp-and-canonical-filters');

    await call('Emulation.setScriptExecutionDisabled', { value: false });
    await viewport(1280, 900);
    await go(all);
    await capture('desktop-reviews');
    const { targetId } = await call('Target.createTarget', { url: 'about:blank' });
    const { sessionId } = await call('Target.attachToTarget', { targetId, flatten: true });
    await go(all, sessionId);
    for (const phrase of ['Đã duyệt đánh giá.', 'Đã ẩn đánh giá.']) {
      await evaluate(`document.querySelector(${JSON.stringify(button)}).click()`, 'Moderate in second tab', sessionId);
      await until(async () => assert.ok(await evaluate(
        `document.readyState === 'complete' && document.querySelector('[data-page-notices]')?.textContent.includes(${JSON.stringify(phrase)})`,
        'Wait for second tab confirmation', sessionId)));
    }
    await call('Target.closeTarget', { targetId });
    await call('Page.bringToFront');
    await submit('staleTwoTabs', 'Đánh giá đã thay đổi.', false);
    assert.equal(trace.staleTwoTabs.after.state, 'Đã ẩn');
    assert.equal(trace.staleTwoTabs.after.focus, 'row-button');
    checks.push('two-tabs-reject-stale-approval-and-load-latest-revision');

    const revision = `${row} input[name="revision"]`;
    const currentRevision = async () => Number(await evaluate(`document.querySelector(${JSON.stringify(revision)}).value`));
    const beforeLostResponse = await currentRevision();
    const action = await evaluate(`document.querySelector(${JSON.stringify(button)}).form.action`);
    let postResponses = 0;
    let rejectedResponse;
    const loseResponse = event => {
      const { method, params } = JSON.parse(event.data);
      if (method !== 'Fetch.requestPaused') return;
      if (params.request.method === 'POST') {
        postResponses++;
        rejectedResponse = call('Fetch.failRequest', { requestId: params.requestId, errorReason: 'TimedOut' });
      } else {
        call('Fetch.continueRequest', { requestId: params.requestId });
      }
    };
    socket.addEventListener('message', loseResponse);
    await call('Fetch.enable', { patterns: [{ urlPattern: action, requestStage: 'Response' }] });
    await focusButton();
    await key('Enter', 13);
    await until(async () => assert.equal(postResponses, 1));
    await rejectedResponse;
    await call('Fetch.disable');
    socket.removeEventListener('message', loseResponse);
    await go(all);
    await until(async () => assert.equal(await currentRevision(), beforeLostResponse + 1));
    assert.equal((await snapshot()).state, 'Đang hiện');
    checks.push('lost-post-response-recovered-by-get-with-one-mutation');

    await go('/admin/orders');
    const history = await call('Page.getNavigationHistory');
    const previous = history.entries[history.currentIndex - 1];
    assert.equal(previous.url, base + all);
    await call('Page.navigateToHistoryEntry', { entryId: previous.id });
    await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), base + all + '|complete'));
    assert.equal(await currentRevision(), beforeLostResponse + 1);
    checks.push('browser-back-restores-filtered-get-without-resubmitting');

    const pid = Number(mongoPid);
    assert.ok(Number.isInteger(pid) && pid > 0);
    process.kill(pid);
    await submit('outageWrite', 'Hãy tải lại để kiểm tra trạng thái', false);
    assert.equal(trace.outageWrite.after.focus, 'review-results');
    assert.ok(await evaluate('document.body.innerText.includes("Tạm thời chưa tải được đánh giá") && !document.body.innerText.includes("Không có đánh giá phù hợp")'));
    assert.ok(await evaluate('!document.querySelector(".admin-review-list button")'));
    await capture('desktop-unavailable');
    checks.push('mongo-read-and-write-outage-is-not-empty-or-success');
    await writeFile(evidence, JSON.stringify({ browser: Browser, checks, trace }, null, 2) + '\n');
  }
} finally {
  socket?.close();
  chrome.kill();
}
