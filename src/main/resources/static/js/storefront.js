const variant = document.querySelector('[data-submit-variant]');
variant?.addEventListener('change', () => variant.form.requestSubmit());
