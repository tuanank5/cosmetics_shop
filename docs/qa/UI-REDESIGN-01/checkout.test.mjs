import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import vm from 'node:vm';

const script = await readFile(new URL('../../../src/main/resources/static/js/checkout.js', import.meta.url), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

class Element extends EventTarget {
  dataset = {};
  attributes = new Map();
  children = new Map();
  childNodes = [];
  hidden = false;
  disabled = false;
  value = '';
  textContent = '';

  constructor(id) {
    super();
    this.id = id;
  }

  querySelector(selector) { return this.children.get(selector) ?? null; }
  querySelectorAll() { return [...this.children.values()]; }
  setAttribute(key, value) { this.attributes.set(key, value); }
  getAttribute(key) { return this.attributes.get(key) ?? null; }
  removeAttribute(key) { this.attributes.delete(key); }
  replaceChildren(...nodes) { this.childNodes = nodes; }
  focus() { this.focused = true; }
}

function response({ addressId = '1', voucher = '', total = '175.000 ₫', invalid = false, error = '' } = {}) {
  const totals = new Element();
  totals.dataset.quoteValid = String(!invalid);
  totals.replaceWith = () => {};
  const amount = new Element();
  amount.textContent = total;
  totals.children.set('[data-checkout-total]', amount);
  const order = new Element();
  order.elements = { addressId: { value: invalid ? '' : addressId }, voucher: { value: invalid ? '' : voucher } };
  const errors = new Element();
  const renderedVoucher = new Element('voucher');
  if (error) renderedVoucher.setAttribute('aria-invalid', 'true');
  const fields = {
    voucher: renderedVoucher, addressId: new Element('addressId'),
    'voucher-error': Object.assign(new Element(), { textContent: error }),
  };
  return {
    ok: true, text: async () => ({
      getElementById: id => fields[id] || null,
      querySelector: selector => ({
        '[data-quote-region]': totals, '#checkout-order': order, '[data-quote-errors]': errors,
      })[selector],
    }),
  };
}

function fixture() {
  const window = new EventTarget();
  const quote = new Element();
  quote.action = 'http://127.0.0.1/checkout/quote';
  const address = new Element('addressId');
  address.value = '1';
  const voucher = new Element('voucher');
  quote.elements = { addressId: address, voucher };
  const errors = new Element();
  const fallback = new Element();
  quote.children.set('[data-quote-errors]', errors);
  quote.children.set('[data-quote-fallback]', fallback);
  const order = new Element();
  order.elements = { addressId: { value: '1' }, voucher: { value: '' } };
  const place = new Element();
  order.children.set('[data-place-order]', place);
  const status = new Element();
  const retry = new Element();
  const initial = new Element();
  initial.dataset.quoteValid = 'true';
  initial.replaceWith = () => {};
  const fieldErrors = { 'addressId-error': new Element('addressId-error'), 'voucher-error': new Element('voucher-error') };
  const document = {
    getElementById: id => fieldErrors[id],
    querySelector: selector => ({
      '[data-checkout-form]': quote, '#checkout-order': order, '[data-quote-status]': status,
      '[data-quote-retry]': retry, '[data-quote-region]': initial,
    })[selector],
  };
  let timerId = 0;
  const timers = new Map();
  const fire = ms => {
    for (const [id, timer] of timers) {
      if (timer.ms !== ms) continue;
      timers.delete(id);
      timer.callback();
      return true;
    }
    return false;
  };
  const requests = [];
  const fetch = (url, options) => new Promise((resolve, reject) => requests.push({ url, options, resolve, reject }));
  const FormData = function (form) { return new Map([['addressId', form.elements.addressId.value], ['voucher', form.elements.voucher.value], ['_csrf', 'fixture']]); };
  const DOMParser = class { parseFromString(doc) { return doc; } };
  Object.assign(window, { fetch, AbortController, DOMParser });
  vm.runInNewContext(script, {
    window, document, fetch, AbortController, DOMParser, FormData, URLSearchParams,
    setTimeout: (callback, ms) => { timers.set(++timerId, { callback, ms }); return timerId; },
    clearTimeout: id => timers.delete(id),
  });
  return { address, voucher, place, order, status, retry, fallback, fieldErrors, fire, requests };
}
const send = (element, type) => element.dispatchEvent(new Event(type, { cancelable: true }));

test('SSR enables the confirmed order and script hides the no-JavaScript control', () => {
  const f = fixture();
  assert.equal(f.fallback.hidden, true);
  assert.equal(f.place.disabled, false);
  assert.equal(send(f.order, 'submit'), true);
});

test('address is quoted immediately and voucher changes are debounced', async () => {
  const f = fixture();
  f.address.value = '2';
  send(f.address, 'change');
  assert.equal(send(f.order, 'submit'), false);
  assert.equal(f.place.disabled, true);
  assert.match(f.status.textContent, /Đang kiểm tra/);
  assert.equal(f.fire(0), true);
  assert.equal(f.requests[0].options.body.get('addressId'), '2');
  assert.equal(f.requests[0].options.body.get('_csrf'), 'fixture');
  f.requests[0].resolve(response({ addressId: '2' }));
  await flush();
  assert.equal(f.place.disabled, false);

  f.voucher.value = 'F';
  send(f.voucher, 'input');
  f.voucher.value = 'FIXTURE';
  send(f.voucher, 'input');
  assert.equal(f.fire(0), false);
  assert.equal(f.fire(350), true);
  assert.equal(f.fire(350), false);
  assert.equal(f.requests[1].options.body.get('voucher'), 'FIXTURE');
  f.requests[1].resolve(response({ addressId: '2', voucher: 'FIXTURE', total: '165.000 ₫' }));
  await flush();
  assert.match(f.status.textContent, /165\.000/);
  assert.equal(f.order.elements.voucher.value, 'FIXTURE');
});

test('a slower quote cannot overwrite newer totals, including when its transport ignores abort', async () => {
  const f = fixture();
  f.voucher.value = 'FIRST';
  send(f.voucher, 'input');
  f.fire(350);
  f.voucher.value = '';
  send(f.voucher, 'input');
  f.fire(350);
  assert.equal(f.requests[0].options.signal.aborted, true);
  assert.equal(f.requests[1].options.body.get('voucher'), '');
  f.requests[1].resolve(response());
  await flush();
  f.requests[0].resolve(response({ voucher: 'FIRST', total: '100.000 ₫' }));
  await flush();
  assert.match(f.status.textContent, /175\.000/);
  assert.equal(f.order.elements.voucher.value, '');
  assert.equal(f.place.disabled, false);
});

test('a field error associates feedback and keeps the typed voucher', async () => {
  const f = fixture();
  f.voucher.value = 'INVALID';
  send(f.voucher, 'input');
  f.fire(350);
  f.requests[0].resolve(response({ invalid: true, error: 'Mã không hợp lệ.' }));
  await flush();
  assert.equal(f.voucher.value, 'INVALID');
  assert.equal(f.voucher.getAttribute('aria-invalid'), 'true');
  assert.equal(f.fieldErrors['voucher-error'].textContent, 'Mã không hợp lệ.');
  assert.equal(f.place.disabled, true);
});

test('timeout requires an explicit quote retry and never posts an order', async () => {
  const f = fixture();
  f.voucher.value = 'FIXTURE';
  send(f.voucher, 'input');
  f.fire(350);
  assert.equal(f.fire(8000), true);
  assert.equal(f.requests[0].options.signal.aborted, true);
  f.requests[0].reject(new Error('timeout'));
  await flush();
  assert.match(f.status.textContent, /phản hồi quá lâu/);
  assert.equal(f.retry.hidden, false);
  assert.equal(f.place.disabled, true);
  assert.equal(f.requests.length, 1);
  send(f.retry, 'click');
  f.fire(0);
  assert.equal(f.requests.length, 2);
  for (const req of f.requests) assert.match(req.url, /\/checkout\/quote$/);
  f.requests[1].resolve(response({ voucher: 'FIXTURE' }));
  await flush();
  assert.equal(f.place.disabled, false);
});
