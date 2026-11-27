(() => {
  'use strict';
  const form = document.querySelector('[data-chat-form]');
  if (!form) return;

  const question = form.elements.cauHoi;
  const status = form.querySelector('[data-chat-status]');
  document.querySelectorAll('[data-chat-prompt]').forEach(button => {
    button.addEventListener('click', () => {
      question.value = button.dataset.chatPrompt;
      question.focus();
    });
    button.hidden = false;
  });
  document.querySelectorAll('[data-chat-example]').forEach(example => { example.hidden = true; });
  form.addEventListener('submit', event => {
    if (!event.defaultPrevented) status.textContent = 'BeautyShop đang xử lý câu hỏi…';
  });
  window.addEventListener('pageshow', () => { status.textContent = ''; });
  document.querySelector('[data-chat-answer]')?.focus();
})();
