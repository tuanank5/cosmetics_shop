(() => {
  'use strict';

  const form = document.querySelector('[data-checkout-form]');
  form?.querySelector('.error-summary')?.focus();
  if (!form?.elements.addressId || !window.fetch || !window.AbortController || !window.DOMParser) return;

  const address = form.elements.addressId;
  const voucher = form.elements.voucher;
  const order = document.querySelector('#checkout-order');
  const place = order.querySelector('[data-place-order]');
  const status = document.querySelector('[data-quote-status]');
  const retry = document.querySelector('[data-quote-retry]');
  const errors = form.querySelector('[data-quote-errors]');
  const ship = form.elements.ship;
  const fields = [address, voucher, ship];
  let region = document.querySelector('[data-quote-region]');
  let version = 0;
  let timer;
  let active;
  const expired = new Error('Phiên thanh toán đã hết hạn. Hãy tải lại trang để tiếp tục.');

  function current() {
    return region.dataset.quoteValid === 'true'
      && order.elements.addressId.value === address.value
      && order.elements.ship.value.trim() === ship.value.trim()
      && order.elements.voucher.value === voucher.value.trim();
  }

  function hideQuote() {
    region.dataset.quoteValid = 'false';
    region.dataset.quotePending = 'true';
    region.querySelectorAll('[data-quote-confirmed], [data-checkout-discount], [data-checkout-shipping], [data-checkout-total]')
      .forEach(item => { item.hidden = true; });
    place.disabled = true;
  }

  function showFieldErrors(source) {
    for (const field of fields) {
      const error = document.getElementById(field.id + '-error');
      if (!error) continue;
      error.hidden = !source?.getElementById(error.id)?.textContent;
      error.textContent = source?.getElementById(error.id)?.textContent || '';
      if (source?.getElementById(field.id)?.getAttribute('aria-invalid') === 'true') {
        field.setAttribute('aria-invalid', 'true');
      } else {
        field.removeAttribute('aria-invalid');
      }
    }
  }

  async function requestQuote(requestVersion) {
    const controller = new AbortController();
    active = controller;
    const timeout = setTimeout(() => controller.abort(), 8000);
    const body = new URLSearchParams(new FormData(form));
    try {
      const response = await fetch(form.action, {
        method: 'POST', credentials: 'same-origin', cache: 'no-store',
        body, signal: controller.signal,
      });
      if (response.redirected || response.status === 401 || response.status === 403) {
        throw expired;
      }
      if (!response.ok) throw new Error();
      const source = new DOMParser().parseFromString(await response.text(), 'text/html');
      if (requestVersion !== version) return;
      if (controller.signal.aborted) throw new Error();
      const next = source.querySelector('[data-quote-region]');
      const nextOrder = source.querySelector('#checkout-order');
      const nextErrors = source.querySelector('[data-quote-errors]');
      if (!next || !nextOrder || !nextErrors) throw new Error();

      region.replaceWith(next);
      region = next;
      errors.replaceChildren(...nextErrors.childNodes);
      showFieldErrors(source);
      order.elements.addressId.value = nextOrder.elements.addressId.value;
      order.elements.voucher.value = nextOrder.elements.voucher.value;
      order.elements.ship.value = nextOrder.elements.ship.value;
      place.disabled = !current();
      const total = region.querySelector('[data-checkout-total]');
      status.textContent = current()
        ? 'Đã cập nhật báo giá. Tổng thanh toán: ' + total.textContent.trim() + '.'
        : 'Tổng thanh toán chưa xác nhận. Kiểm tra thông tin ở trên.';
      retry.hidden = !errors.querySelector('[data-quote-unavailable]');
    } catch (error) {
      if (requestVersion !== version) return;
      status.textContent = controller.signal.aborted
        ? 'Báo giá phản hồi quá lâu. Tổng thanh toán chưa xác nhận.'
        : error === expired ? expired.message : 'Chưa nhận được báo giá. Kiểm tra kết nối và thử lại.';
      retry.hidden = false;
    } finally {
      clearTimeout(timeout);
    }
  }

  function schedule(delay) {
    const requestVersion = ++version;
    clearTimeout(timer);
    active?.abort();
    hideQuote();
    errors.replaceChildren();
    showFieldErrors();
    retry.hidden = true;
    if (!address.value) {
      status.textContent = 'Chọn địa chỉ giao hàng để xem báo giá.';
      return;
    }
    status.textContent = 'Đang kiểm tra báo giá…';
    timer = setTimeout(() => requestQuote(requestVersion), delay);
  }

  form.addEventListener('submit', event => {
    event.preventDefault();
    event.stopImmediatePropagation();
    schedule(0);
  }, true);
  order.addEventListener('submit', event => {
    if (current()) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    schedule(0);
  }, true);
  function showSelectedAddress() {
    form.querySelectorAll('[data-address-preview]').forEach(preview => {
      if (preview.dataset.addressPreview != null) preview.hidden = preview.dataset.addressPreview !== address.value;
    });
  }
  address.addEventListener('change', () => {
    showSelectedAddress();
    schedule(0);
  });
  ship.addEventListener('input', () => schedule(350));
  voucher.addEventListener('input', () => schedule(350));
  retry.addEventListener('click', () => {
    schedule(0);
    status.focus();
  });
  window.addEventListener('pageshow', event => {
    if (!event.persisted) return;
    showSelectedAddress();
    if (!current()) schedule(0);
  });
  showSelectedAddress();
  form.querySelector('[data-quote-fallback]').hidden = true;
  const enhancedHelp = form.querySelector('[data-quote-enhanced]');
  if (enhancedHelp) enhancedHelp.hidden = false;
  retry.hidden = !errors.querySelector('[data-quote-unavailable]');
})();
