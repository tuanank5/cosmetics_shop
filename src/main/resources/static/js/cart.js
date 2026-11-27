(() => {
  'use strict';

  const page = document.querySelector('.cart-page');
  const forms = page?.querySelectorAll('form.cart-edit');
  const recovery = page?.querySelector('[data-cart-recovery]');
  if (!forms?.length || !recovery || !window.fetch || !window.AbortController || !window.DOMParser) return;

  const reload = recovery.querySelector('a');
  const cartUrl = new URL(reload.href);
  const recoveryText = recovery.querySelector('[role="alert"]');
  const live = page.querySelector('[data-cart-live]');
  const dialog = document.querySelector('[data-confirm-dialog]');
  const checkout = page.querySelector('[data-cart-checkout]');
  const states = new Map();
  const pending = new Set();
  const changes = 'BroadcastChannel' in window ? new BroadcastChannel('beautyshop:cart:' + cartUrl.pathname) : null;
  const debounceMs = 350;
  const timeoutMs = 8000;
  let timer;
  let sending;

  const markBusy = (state, on) => {
    if (on) state.line.setAttribute('aria-busy', 'true');
    else state.line.removeAttribute('aria-busy');
  };
  const syncInput = (state, value) => {
    state.input.value = value;
    state.input.dispatchEvent(new Event('input', { bubbles: true }));
  };
  const stop = (message) => {
    if (!recovery.hidden) return;
    clearTimeout(timer);
    pending.clear();
    live.textContent = '';
    recovery.hidden = false;
    recoveryText.textContent = message;
    checkout?.removeAttribute('href');
    checkout?.setAttribute('role', 'link');
    checkout?.setAttribute('aria-disabled', 'true');
    if (page.contains(document.activeElement)) reload.focus();
    page.querySelector('.cart-lines').inert = true;
    states.forEach(state => markBusy(state, false));
  };
  const stale = () => stop('Giỏ hàng có thể đã thay đổi. Hãy xem lại số lượng và tiền hàng trước khi tiếp tục.');
  changes?.addEventListener('message', stale);
  window.addEventListener('pageshow', event => {
    if (event.persisted) stale();
  });

  const textOf = (node) => (node?.textContent || '').replace(/\s+/g, ' ').trim();
  const apply = (doc, changed, sent) => {
    const freshLines = new Map([...doc.querySelectorAll('.cart-page [data-cart-line]')]
      .map(line => [line.dataset.cartLine, line]));
    if (freshLines.size !== states.size || [...states.values()].some(state => !freshLines.has(state.line.dataset.cartLine))) {
      stale();
      return;
    }
    const move = (current, fresh) => {
      if (current && fresh) current.replaceWith(document.importNode(fresh, true));
    };

    const failure = textOf(doc.querySelector('.cart-page .alert--error'));
    states.forEach(state => {
      const fresh = freshLines.get(state.line.dataset.cartLine);
      const freshInput = fresh.querySelector('[name="quantity"]');
      const draft = state === changed ? state.input.value !== sent : state.input.value !== state.server;
      state.server = freshInput.value;
      state.input.max = freshInput.max;
      move(state.line.querySelector('.cart-prices'), fresh.querySelector('.cart-prices'));
      move(state.line.querySelector('.cart-stock'), fresh.querySelector('.cart-stock'));
      syncInput(state, draft ? state.input.value : state.server);
    });
    move(page.querySelector('.cart-summary dl'), doc.querySelector('.cart-summary dl'));
    const badge = document.querySelector('[data-cart-link]');
    const freshBadge = doc.querySelector('[data-cart-link]');
    if (badge && freshBadge) {
      badge.setAttribute('aria-label', freshBadge.getAttribute('aria-label'));
      move(badge.querySelector('.cart-count'), freshBadge.querySelector('.cart-count'));
    }

    if (failure || changed.server !== sent) {
      changed.error.textContent = failure || 'Số lượng đã thay đổi trên máy chủ. Hãy kiểm tra lại.';
      return;
    }
    const sku = changed.line.dataset.sku;
    const subtotal = textOf(page.querySelector('.cart-summary dd'));
    live.textContent = 'Đã cập nhật số lượng cho ' + sku + ': ' + sent + '. Tiền hàng ' + subtotal + '.';
  };

  const drain = async () => {
    if (sending || !recovery.hidden) return;
    const state = pending.values().next().value;
    if (!state) return;
    pending.delete(state);
    const { form, input } = state;
    const sent = input.value;
    if (sent === state.server || !input.checkValidity()) {
      markBusy(state, false);
      drain();
      return;
    }
    if (input.valueAsNumber === 0) {
      markBusy(state, false);
      restoreOrRemove(state);
      drain();
      return;
    }
    sending = state;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(form.action, {
        method: 'POST', body: new FormData(form), credentials: 'same-origin', headers: { Accept: 'text/html' },
        signal: controller.signal, cache: 'no-store',
      });
      const target = new URL(response.url);
      if (!response.ok || target.origin !== cartUrl.origin || target.pathname !== cartUrl.pathname) {
        throw new Error('Unexpected cart response');
      }
      const doc = new DOMParser().parseFromString(await response.text(), 'text/html');
      if (!doc.querySelector('.cart-page')) throw new Error('Missing cart page');
      if (recovery.hidden) apply(doc, state, sent);
    } catch {
      stop('Chưa xác nhận được lần cập nhật vừa rồi. Hãy xem lại giỏ hàng để kiểm tra số lượng đã lưu.');
    } finally {
      clearTimeout(timeout);
      sending = null;
      changes?.postMessage('changed');
      markBusy(state, pending.has(state));
      drain();
    }
  };

  const schedule = (state, delay) => {
    if (!recovery.hidden) return;
    pending.add(state);
    markBusy(state, true);
    clearTimeout(timer);
    timer = setTimeout(drain, delay);
  };
  const markZero = (state) => {
    state.returnFocus = state.form.contains(document.activeElement) ? document.activeElement : state.input;
  };
  const cancelZero = (state) => {
    syncInput(state, state.server);
    state.returnFocus?.focus();
    state.returnFocus = null;
  };
  const restoreOrRemove = (state) => {
    markZero(state);
    state.form.requestSubmit();
    if (!dialog?.open && !state.form.dataset.submitting) cancelZero(state);
  };
  const waitForCart = (event) => {
    if (recovery.hidden && !sending && !pending.size) return false;
    event.preventDefault();
    event.stopPropagation();
    if (recovery.hidden) live.textContent = 'Đang cập nhật giỏ hàng. Vui lòng chờ.';
    else reload.focus();
    return true;
  };

  forms.forEach(form => {
    const input = form.elements.quantity;
    const state = {
      form, input, line: form.closest('[data-cart-line]'), error: form.querySelector('.field-error'),
      server: input.value, returnFocus: null,
    };
    states.set(form, state);
    form.dataset.cartAuto = 'true';
    form.querySelector('button[type="submit"]').hidden = true;
    form.querySelectorAll('[data-cart-step]').forEach(button => {
      button.addEventListener('click', () => schedule(state, debounceMs));
    });
    input.addEventListener('change', () => schedule(state, debounceMs));
  });

  dialog?.addEventListener('close', () => {
    states.forEach(state => {
      if (!state.returnFocus) return;
      if (state.form.dataset.submitting) state.returnFocus = null;
      else cancelZero(state);
    });
    drain();
  });
  page.addEventListener('submit', event => {
    const state = states.get(event.target);
    if (!state) {
      if (event.target.matches('.cart-remove')) waitForCart(event);
      return;
    }
    if (state.input.valueAsNumber === 0) {
      pending.delete(state);
      markBusy(state, sending === state);
      if (waitForCart(event)) syncInput(state, state.server);
      else markZero(state);
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    schedule(state, 0);
  }, true);
  checkout?.addEventListener('click', waitForCart);
})();
