import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';
import vm from 'node:vm';

const [file, phase] = process.argv.slice(2);
const script = new vm.Script(await readFile(file, 'utf8'), { filename: 'cart.js' });
const root = 'http://127.0.0.1:12345';

class Element {
  constructor(properties = {}) {
    this.dataset = {};
    this.children = new Map();
    this.events = new Map();
    this.attributes = new Map();
    Object.assign(this, properties);
  }
  querySelector(selector) { return this.children.get(selector) ?? null; }
  querySelectorAll(selector) { return this.children.get(selector) ?? []; }
  closest() { return this.line; }
  replaceWith(other) { this.textContent = other.textContent; }
  setAttribute(name, value) { this.attributes.set(name, value); }
  removeAttribute(name) { this.attributes.delete(name); }
  getAttribute(name) { return this.attributes.get(name); }
  dispatchEvent(event) { this.events.get(event.type)?.(event); }
  addEventListener(name, listener) { this.events.set(name, listener); }
}

const currency = quantities => quantities[0] * 100000 + quantities[1] * 150000;
const text = textContent => new Element({ textContent: String(textContent) });
const quote = quantities => new Element({ children: new Map([['.cart-summary dd', text(currency(quantities))]]) });

function response(quantities) {
  const items = quantities.map((quantity, i) => {
    const input = new Element({ value: String(quantity), max: '10' });
    return new Element({ dataset: { cartLine: String(i + 1) },
      children: new Map([
        ['input[name="quantity"]', input], ['[name="quantity"]', input],
        ['.cart-prices', text(quantity * (i === 0 ? 100000 : 150000))], ['.cart-stock', text('10')],
      ]),
    });
  });
  const cartBadge = new Element({ children: new Map([['.cart-count', text(quantities[0] + quantities[1])]]) });
  cartBadge.setAttribute('aria-label', String(quantities[0] + quantities[1]));
  const summary = quote(quantities);
  return new Element({ children: new Map([
    ['.cart-page', new Element()], ['.cart-page [data-cart-line]', items],
    ...items.map((item, i) => ['.cart-page [data-cart-line="' + (i + 1) + '"]', item]),
    ['.cart-page .cart-summary dl', summary], ['.cart-summary dl', summary], ['[data-cart-link]', cartBadge],
    ['.site-header a.header-action[href="/cart"]', cartBadge],
  ]) });
}

function fixture(mode, notify) {
  const server = [1, 1];
  const requests = [];
  let release;
  let legacyPosts = 0;
  let displaySubtotal = currency(server);
  let displayBadge = 2;
  let now = 0;
  let count = 0;
  let focus = false;
  let onAnotherTab;
  const timers = new Map();
  const lines = new Element();
  const checkout = new Element();
  checkout.setAttribute('href', '/checkout');
  const recovery = new Element({ hidden: true,
    children: new Map([
      ['a', new Element({ href: root + '/cart', focus: () => { focus = true; } })],
      ['[role="alert"]', text('')],
    ]),
  });
  const forms = server.map((value, i) => {
    const input = new Element({ value: String(value), checkValidity: () => true });
    Object.defineProperty(input, 'valueAsNumber', { get: () => +input.value });
    const line = new Element({ dataset: { cartLine: String(i + 1), sku: 'FIXTURE-' + (i + 1) },
      children: new Map([['.cart-prices', text(0)], ['.cart-stock', text(10)],
        ['.cart-prices dd', [text(0), text(0)]]]),
    });
    return new Element({
      line, elements: { quantity: input }, action: root + '/cart/items/' + (i + 1) + '/quantity',
      children: new Map([
        ['.field-error', text('')], ['button[type="submit"]', new Element()],
        ['[data-cart-step]', [new Element()]],
      ]),
    });
  });
  const summary = {
    replaceWith(fresh) { displaySubtotal = +fresh.querySelector('.cart-summary dd').textContent; },
  };
  const badge = new Element({
    children: new Map([['.cart-count', { replaceWith: fresh => { displayBadge = +fresh.textContent; } }]]),
  });
  badge.replaceWith = fresh => { displayBadge = +fresh.querySelector('.cart-count').textContent; };
  const page = new Element({
    contains: node => forms.some(form => form.elements.quantity === node),
    children: new Map([
      ['form.cart-edit', forms], ['[data-cart-recovery]', recovery], ['[data-cart-live]', text('')],
      ['.cart-lines', lines], ['[data-cart-checkout]', checkout], ['.cart-summary dl', summary],
    ]),
  });
  page.querySelector = (selector) => selector === '.cart-summary dd' ? text(displaySubtotal) : page.children.get(selector) ?? null;
  const document = new Element({ activeElement: forms[0].elements.quantity, importNode: node => node,
    children: new Map([
      ['.cart-page', page], ['[data-cart-link]', badge], ['.site-header a.header-action[href="/cart"]', badge],
    ]),
  });
  const fetch = async (url, options) => {
    const sku = +url.split('/').at(-2);
    const quantity = +options.body.form.elements.quantity.value;
    requests.push([sku, quantity]);
    if (mode === 'hung') {
      await new Promise((_, reject) => options.signal?.addEventListener('abort', () => reject(options.signal.reason)));
    }
    server[sku - 1] = quantity;
    const snapshot = response(server);
    if (mode === 'lost') throw new TypeError('Fixture lost the server response');
    if (mode === 'delay' && requests.length === 1) {
      await new Promise(resolve => { release = resolve; });
    }
    return { ok: true, url: root + '/cart', text: async () => snapshot };
  };
  const BroadcastChannel = class {
    addEventListener(_, callback) { onAnotherTab = callback; }
    postMessage(message) { notify?.(message); }
  };
  const DOMParser = class { parseFromString(doc) { return doc; } };
  const FormData = class { constructor(form) { this.form = form; } };
  const HTMLFormElement = class { submit() { legacyPosts++; } };
  const window = { fetch, FormData, DOMParser, BroadcastChannel, AbortController, addEventListener() {} };
  script.runInNewContext({
    window, document, fetch, AbortController, BroadcastChannel, DOMParser, FormData, HTMLFormElement,
    location: { href: root + '/cart' }, URL, CSS: { escape: value => value }, Event, console,
    setTimeout(callback, ms) { timers.set(++count, { callback, when: now + ms }); return count; },
    clearTimeout(id) { timers.delete(id); },
  });
  return {
    requests, server,
    async tick(ms) {
      const end = now + ms;
      for (;;) {
        const matches = [...timers].filter(([, timer]) => timer.when <= end).sort((a, b) => a[1].when - b[1].when);
        if (!matches.length) break;
        const [id, timer] = matches[0];
        now = timer.when;
        timers.delete(id);
        timer.callback();
        await new Promise(setImmediate);
      }
      now = end;
    },
    setQuantity(sku, quantity) {
      const form = forms[sku - 1];
      form.elements.quantity.value = String(quantity);
      form.querySelectorAll('[data-cart-step]')[0].dispatchEvent({ type: 'click' });
    },
    async release() {
      release();
      await new Promise(setImmediate);
    },
    incoming: () => onAnotherTab?.(),
    view() {
      return {
        subtotal: displaySubtotal, badge: displayBadge, legacyPosts, recovery: !recovery.hidden,
        busy: forms.some(form => form.line.getAttribute('aria-busy') === 'true'),
        focus, checkoutAvailable: checkout.getAttribute('href') != null, inert: Boolean(lines.inert),
      };
    },
  };
}

const fixed = phase === 'after';
const results = { phase, environment: 'Node VM, simulated DOM, clock and transport' };
const racing = fixture('delay');
racing.setQuantity(1, 2);
await racing.tick(350);
racing.setQuantity(2, 2);
await racing.tick(350);
assert.equal(racing.requests.length, fixed ? 1 : 2);
await racing.release();
await racing.tick(500);
assert.deepEqual(racing.server, [2, 2]);
assert.equal(racing.view().subtotal, fixed ? 500000 : 350000);
assert.equal(racing.view().badge, fixed ? 4 : 3);
results.reversed = { requests: racing.requests, displayed: racing.view(), serverSubtotal: currency(racing.server) };

const latest = fixture('delay');
latest.setQuantity(1, 2);
await latest.tick(350);
latest.setQuantity(1, 3);
await latest.tick(350);
await latest.release();
await latest.tick(500);
assert.deepEqual(latest.requests, [[1, 2], [1, 3]]);
assert.equal(latest.view().subtotal, 450000);
results.latest = { requests: latest.requests, displayed: latest.view() };

const lost = fixture('lost');
lost.setQuantity(1, 2);
await lost.tick(350);
assert.equal(lost.server[0], 2);
assert.equal(lost.view().legacyPosts, fixed ? 0 : 1);
assert.equal(lost.view().recovery, fixed);
results.lost = { requests: lost.requests, displayed: lost.view(), server: lost.server };

const hung = fixture('hung');
hung.setQuantity(1, 2);
await hung.tick(9000);
assert.equal(hung.view().busy, !fixed);
assert.equal(hung.view().recovery, fixed);
assert.equal(hung.view().checkoutAvailable, !fixed);
assert.equal(hung.view().focus, fixed);
results.hung = { requests: hung.requests, displayed: hung.view() };

const secondTab = fixture('normal');
const firstTab = fixture('normal', () => secondTab.incoming());
firstTab.setQuantity(1, 2);
await firstTab.tick(350);
secondTab.setQuantity(2, 2);
await secondTab.tick(350);
assert.equal(secondTab.view().recovery, fixed);
assert.equal(secondTab.requests.length, fixed ? 0 : 1);
results.anotherTab = { requests: secondTab.requests, displayed: secondTab.view() };

await writeFile('docs/qa/BIZ-UX-01/cart-vm-' + phase + '.json', JSON.stringify(results, null, 2) + '\n');
