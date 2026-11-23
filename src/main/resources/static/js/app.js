(() => {
  'use strict';
  const menu = document.querySelector('.header-menu');
  if (menu) {
    const desktop = window.matchMedia('(min-width: 1100px)');
    menu.open = desktop.matches;
    desktop.addEventListener('change', () => {
      if (menu.contains(document.activeElement)) {
        menu.querySelector('summary').focus();
      }
      menu.querySelectorAll('details[open]').forEach((group) => { group.open = false; });
      menu.open = desktop.matches;
    });
    menu.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && menu.open) {
        const expanded = event.target.closest('details[open]');
        if (expanded) {
          expanded.open = false;
          expanded.querySelector('summary').focus();
          event.preventDefault();
        }
      }
    });
  }
  const skuImage = document.querySelector('[data-sku-image]');
  if (skuImage) {
    const showPlaceholder = () => {
      skuImage.hidden = true;
      skuImage.parentElement.querySelector('.store-placeholder').hidden = false;
    };
    skuImage.addEventListener('error', showPlaceholder, { once: true });
    if (skuImage.complete && skuImage.naturalWidth === 0) showPlaceholder();
  }

  document.querySelectorAll('[data-catalog-image]').forEach((image) => {
    const placeholder = image.parentElement.querySelector('[data-catalog-placeholder]');
    if (!placeholder) return;
    const showPlaceholder = () => {
      image.hidden = true;
      placeholder.hidden = false;
    };
    image.addEventListener('error', showPlaceholder, { once: true });
    if (image.complete && image.naturalWidth === 0) showPlaceholder();
  });

  document.querySelectorAll('.cart-edit').forEach((form) => {
    const input = form.elements.quantity;
    const error = form.querySelector('.field-error');
    const steps = form.querySelectorAll('[data-cart-step]');
    const refresh = () => {
      const value = input.valueAsNumber;
      const maximum = Number(input.max);
      steps.forEach((button) => {
        button.disabled = button.dataset.cartStep === '1' ? value >= maximum || maximum === 0 : value <= 0;
      });
      if (input.validity.valid) {
        input.removeAttribute('aria-invalid');
        error.textContent = '';
      } else if (input.hasAttribute('aria-invalid')) {
        error.textContent = `Nhập số nguyên từ 0 đến ${input.max}.`;
      }
    };
    steps.forEach((button) => {
      button.hidden = false;
      button.addEventListener('click', () => {
        if (button.dataset.cartStep === '1') input.stepUp();
        else input.stepDown();
        refresh();
      });
    });
    input.addEventListener('input', refresh);
    input.addEventListener('invalid', (event) => {
      event.preventDefault();
      input.setAttribute('aria-invalid', 'true');
      refresh();
      input.focus();
    });
    form.addEventListener('submit', () => {
      if (input.valueAsNumber === 0) form.dataset.confirm = form.dataset.zeroConfirm;
      else delete form.dataset.confirm;
    });
    refresh();
  });

  const catalogFilters = document.querySelector('[data-catalog-filters]');
  if (catalogFilters) {
    const targets = ['#categories', '#brands', '#skin-type'];
    catalogFilters.open = targets.includes(location.hash);
    window.addEventListener('hashchange', () => {
      if (targets.includes(location.hash)) catalogFilters.open = true;
    });
  }

  document.querySelectorAll('[data-password-toggle]').forEach((button) => {
    const input = document.getElementById(button.dataset.passwordToggle);
    if (!input) return;
    button.hidden = false;
    button.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      button.textContent = show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu';
      button.setAttribute('aria-pressed', String(show));
    });
  });

  const dialog = document.querySelector('[data-confirm-dialog]');
  let pending = null;
  const closeDialog = () => {
    const previous = pending;
    pending = null;
    dialog.close();
    previous?.submitter?.focus();
  };
  if (dialog && typeof dialog.showModal === 'function') {
    dialog.querySelector('[data-confirm-cancel]').addEventListener('click', closeDialog);
    dialog.addEventListener('cancel', (event) => { event.preventDefault(); closeDialog(); });
    dialog.querySelector('[data-confirm-accept]').addEventListener('click', () => {
      const previous = pending;
      if (!previous) return;
      closeDialog();
      previous.form.dataset.confirmed = 'true';
      previous.form.requestSubmit(previous.submitter || undefined);
    });
  }

  document.querySelectorAll('form[method="post"], form[method="POST"]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      if (form.dataset.submitting) { event.preventDefault(); return; }
      const submitter = event.submitter;
      const message = submitter?.dataset.confirm || form.dataset.confirm;
      if (message && form.dataset.confirmed !== 'true') {
        if (dialog && typeof dialog.showModal === 'function') {
          event.preventDefault();
          pending = { form, submitter };
          dialog.querySelector('[data-confirm-message]').textContent = message;
          dialog.showModal();
          dialog.querySelector('[data-confirm-cancel]').focus();
          return;
        }
        if (!window.confirm(message)) { event.preventDefault(); return; }
      }
      delete form.dataset.confirmed;
      form.dataset.submitting = 'true';
      form.setAttribute('aria-busy', 'true');
      // Keep the submitter enabled: its name/value belongs in the POST payload.
      if (submitter) {
        const original = document.createElement('span');
        original.className = 'btn-label';
        while (submitter.firstChild) original.append(submitter.firstChild);
        const progress = document.createElement('span');
        progress.className = 'btn-progress';
        progress.textContent = 'Đang xử lý…';
        submitter.append(original, progress);
        submitter.classList.add('is-loading');
        submitter.setAttribute('aria-disabled', 'true');
      }
    });
  });

  window.addEventListener('pageshow', () => {
    document.querySelectorAll('form[data-submitting]').forEach((form) => {
      delete form.dataset.submitting;
      form.removeAttribute('aria-busy');
      form.querySelectorAll('.is-loading').forEach((button) => {
        const original = button.querySelector('.btn-label');
        if (original) button.replaceChildren(...original.childNodes);
        button.classList.remove('is-loading');
        button.removeAttribute('aria-disabled');
      });
    });
  });
})();
