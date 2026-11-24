(() => {
  'use strict';

  const form = document.querySelector('[data-checkout-form]');
  const errors = form?.querySelector('.error-summary');
  if (errors) errors.focus();
  if (!form || form.dataset.quoteValid !== 'true') return;

  const invalidate = () => {
    form.dataset.quoteValid = 'false';
    form.querySelector('[data-quote-amounts]').hidden = true;
    form.querySelector('[data-quote-stale]').hidden = false;
    form.querySelectorAll('[data-place-order]').forEach((button) => { button.disabled = true; });
  };
  [form.elements.addressId, form.elements.voucher].forEach((field) => {
    field.addEventListener('input', invalidate);
    field.addEventListener('change', invalidate);
  });
})();
