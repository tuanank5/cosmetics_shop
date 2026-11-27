(() => {
  'use strict';

  const button = document.querySelector('[data-copy-order]');
  if (!button || !navigator.clipboard?.writeText) return;

  const code = document.querySelector('[data-order-code]');
  const status = document.querySelector('[data-order-copy-status]');
  button.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(code.textContent.trim());
      status.textContent = 'Đã sao chép mã đơn hàng.';
    } catch {
      status.textContent = 'Bạn có thể bôi đen mã đơn hàng để sao chép.';
    }
  });
  button.hidden = false;
})();
