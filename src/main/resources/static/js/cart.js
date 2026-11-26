(() => {
  'use strict';
  // P17: progressive enhancement for /cart quantity forms. The existing POST + redirect contract is reused:
  // the form is sent with fetch, the redirected /cart HTML is parsed, and only server-rendered regions are imported.
  // Prices and totals are never computed here. Without JS, the "Cập nhật" button and full POST keep working.
  if (typeof window.fetch !== 'function' || typeof window.DOMParser !== 'function' || typeof FormData !== 'function') return;
  const page = document.querySelector('.cart-page');
  if (!page) return;
  const live = page.querySelector('[data-cart-live]');
  const dialog = document.querySelector('[data-confirm-dialog]');
  const DEBOUNCE_MS = 350;
  const controllers = new Map();

  const cartPath = (url) => { try { return new URL(url, location.href).pathname; } catch { return ''; } };
  const textOf = (node) => (node?.textContent || '').replace(/\s+/g, ' ').trim();
  const adopt = (current, replacement) => {
    if (current && replacement) current.replaceWith(document.importNode(replacement, true));
  };

  const enhance = (form) => {
    const line = form.closest('[data-cart-line]');
    const input = form.elements.quantity;
    if (!line || !input) return;
    const id = line.dataset.cartLine;
    const error = form.querySelector('.field-error');
    const submit = form.querySelector('button[type="submit"]');
    const state = { server: input.value, sent: null, timer: 0, inFlight: false, seq: 0, awaitingZero: false, origin: null };

    form.dataset.cartAuto = 'true';
    if (submit) submit.hidden = true;

    const refreshSteps = () => input.dispatchEvent(new Event('input', { bubbles: true }));
    const restore = () => { input.value = state.server; refreshSteps(); };
    const busy = (on) => { if (on) line.setAttribute('aria-busy', 'true'); else line.removeAttribute('aria-busy'); };
    const fullSubmit = () => { busy(false); HTMLFormElement.prototype.submit.call(form); };

    const markZero = () => {
      clearTimeout(state.timer);
      busy(false);
      state.awaitingZero = true;
      state.origin = form.contains(document.activeElement) ? document.activeElement : input;
    };
    const cancelZero = () => {
      state.awaitingZero = false;
      restore();
      state.origin?.focus();
    };
    const askToRemove = () => {
      markZero();
      // app.js turns a zero quantity into the shared confirmation dialog (data-zero-confirm).
      form.requestSubmit();
      if (!(dialog && dialog.open) && !form.dataset.submitting) cancelZero();
    };

    const apply = (doc, sentValue) => {
      const fresh = doc.querySelector(`.cart-page [data-cart-line="${CSS.escape(id)}"]`);
      if (!fresh) {
        // The line disappeared server-side; show the server's whole cart rather than guessing.
        adopt(page.querySelector('.cart-layout, .cart-empty'), doc.querySelector('.cart-page .cart-layout, .cart-page .cart-empty'));
        adopt(document.querySelector('.site-header a.header-action[href="/cart"]'), doc.querySelector('.site-header a.header-action[href="/cart"]'));
        page.focus();
        return;
      }
      adopt(line.querySelector('.cart-prices'), fresh.querySelector('.cart-prices'));
      adopt(line.querySelector('.cart-stock'), fresh.querySelector('.cart-stock'));
      adopt(page.querySelector('.cart-summary dl'), doc.querySelector('.cart-page .cart-summary dl'));
      adopt(document.querySelector('.site-header a.header-action[href="/cart"]'), doc.querySelector('.site-header a.header-action[href="/cart"]'));
      page.querySelectorAll(':scope > .alert').forEach((alert) => alert.remove());

      const freshInput = fresh.querySelector('input[name="quantity"]');
      state.server = freshInput.value;
      input.max = freshInput.max;
      const failure = doc.querySelector('.cart-page > .alert--error');
      const userMovedOn = input.value !== sentValue;
      if (failure || !userMovedOn) input.value = state.server;
      refreshSteps();
      if (failure) {
        error.textContent = textOf(failure);
        input.setAttribute('aria-invalid', 'true');
        return;
      }
      if (!userMovedOn && live) {
        const sku = line.dataset.sku;
        const lineTotal = textOf(line.querySelectorAll('.cart-prices dd')[1]);
        const subtotal = textOf(page.querySelector('.cart-summary dd'));
        live.textContent = `Đã cập nhật số lượng cho mã biến thể sản phẩm ${sku}: ${state.server}. Thành tiền ${lineTotal}. Tiền hàng ${subtotal}.`;
      }
    };

    const send = async () => {
      clearTimeout(state.timer);
      if (state.inFlight) return; // the latest value is sent once the current request settles
      const value = input.value;
      if (value === state.server || !input.checkValidity()) { busy(false); return; }
      if (input.valueAsNumber === 0) { askToRemove(); return; }
      state.inFlight = true;
      state.sent = value;
      const seq = ++state.seq;
      busy(true);
      let doc = null;
      try {
        const response = await fetch(form.action, {
          method: 'POST', body: new FormData(form), credentials: 'same-origin', headers: { Accept: 'text/html' }
        });
        if (response.ok && cartPath(response.url) === '/cart') {
          doc = new DOMParser().parseFromString(await response.text(), 'text/html');
          if (!doc.querySelector('.cart-page')) doc = null;
        }
      } catch {
        doc = null;
      }
      state.inFlight = false;
      if (!doc) { fullSubmit(); return; } // network, expired session or CSRF 403: fall back to the normal POST
      if (seq === state.seq) apply(doc, value);
      if (input.value !== state.server && input.value !== value && input.checkValidity()) send();
      else busy(false);
    };

    const schedule = (delay) => {
      clearTimeout(state.timer);
      if (input.value === '') return;
      if (!input.checkValidity()) { input.reportValidity(); return; }
      if (input.valueAsNumber === 0) { askToRemove(); return; }
      if (input.value !== state.server || state.inFlight) busy(true);
      state.timer = setTimeout(send, delay);
    };

    form.querySelectorAll('[data-cart-step]').forEach((button) => {
      // app.js has already stepped the value when this listener runs.
      button.addEventListener('click', () => schedule(DEBOUNCE_MS));
    });
    input.addEventListener('change', () => schedule(DEBOUNCE_MS));
    controllers.set(form, { schedule, markZero, isZero: () => input.valueAsNumber === 0 });

    if (dialog) {
      dialog.addEventListener('close', () => {
        if (!state.awaitingZero) return;
        if (form.dataset.submitting) state.awaitingZero = false;
        else cancelZero();
      });
    }
  };

  // Capture phase: runs before app.js's generic submit handler, so Enter in the field is handled here.
  document.addEventListener('submit', (event) => {
    const controller = controllers.get(event.target);
    if (!controller) return;
    if (controller.isZero()) { controller.markZero(); return; } // zero keeps the confirm + full POST flow
    event.preventDefault();
    event.stopPropagation();
    controller.schedule(0);
  }, true);

  page.querySelectorAll('form.cart-edit').forEach(enhance);
})();
