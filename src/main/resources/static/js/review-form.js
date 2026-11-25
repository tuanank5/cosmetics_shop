// P23: bộ đếm ký tự cho nhận xét đánh giá. Không có JS thì số đếm server-render và maxlength vẫn giới hạn.
(() => {
  const source = document.querySelector('[data-char-source]');
  const current = document.querySelector('[data-char-current]');
  if (!source || !current) return;
  let timer = 0;
  const update = () => { current.textContent = String(source.value.length); };
  // aria-live đọc lại sau khi người dùng ngừng gõ, tránh đọc từng phím.
  source.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(update, 400); });
  update();
})();
