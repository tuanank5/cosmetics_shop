(() => {
  'use strict';
  const desktop = matchMedia('(hover: hover) and (pointer: fine)');
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const entries = [];
  let active;
  const toggleVisible = (entry, visible) => {
    if (!visible && document.activeElement === entry.button) entry.tile.querySelector('.store-card').focus();
    entry.button.hidden = !visible;
  };
  const permitted = (value) => {
    if (!value) return null;
    try {
      const url = new URL(value, location.href);
      return url.origin === location.origin && /^\/(img|uploads\/sku)\//.test(url.pathname) ? url.href : null;
    } catch { return null; }
  };
  const allowed = () => desktop.matches && !reduced.matches && !navigator.connection?.saveData && !document.hidden;
  const stop = (entry) => {
    entry.generation++;
    entry.video.pause();
    entry.video.removeAttribute('src');
    entry.video.load();
    entry.tile.classList.remove('is-preview-playing');
    entry.button.textContent = 'Phát video xem trước';
    entry.button.setAttribute('aria-pressed', 'false');
    if (active === entry) active = null;
  };
  const start = async (entry) => {
    if (!allowed() || entry.failed) return;
    if (active === entry) return;
    if (active) stop(active);
    active = entry;
    const generation = ++entry.generation;
    entry.video.muted = true;
    entry.video.src = entry.src;
    try {
      await entry.video.play();
      if (active !== entry || generation !== entry.generation || !allowed()) {
        if (active === entry) stop(entry);
        return;
      }
      entry.tile.classList.add('is-preview-playing');
      entry.button.textContent = 'Tạm dừng video';
      entry.button.setAttribute('aria-pressed', 'true');
    } catch {
      if (generation === entry.generation) { entry.failed = true; stop(entry); }
    }
  };
  document.querySelectorAll('[data-product-preview]').forEach((video) => {
    const tile = video.closest('.product-tile');
    const button = tile?.querySelector('[data-preview-toggle]');
    const src = permitted(video.dataset.videoSrc);
    if (!tile || !button || !src) return;
    const entry = { tile, button, video, src, generation: 0, failed: false };
    const poster = permitted(video.dataset.videoPoster);
    if (poster) video.poster = poster;
    entries.push(entry);
    tile.addEventListener('pointerenter', (event) => { if (event.pointerType === 'mouse') start(entry); });
    tile.addEventListener('pointerleave', () => { if (active === entry) stop(entry); });
    button.addEventListener('click', () => { if (active === entry) stop(entry); else start(entry); });
    video.addEventListener('error', () => {
      if (video.hasAttribute('src')) { entry.failed = true; stop(entry); toggleVisible(entry, false); }
    });
  });
  const refresh = () => {
    if (active) stop(active);
    entries.forEach((entry) => { toggleVisible(entry, allowed() && !entry.failed); });
  };
  desktop.addEventListener('change', refresh);
  reduced.addEventListener('change', refresh);
  document.addEventListener('visibilitychange', refresh);
  window.addEventListener('pagehide', () => { if (active) stop(active); });
  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((records) => {
      if (active && records.some((record) => record.target === active.tile && !record.isIntersecting)) stop(active);
    });
    entries.forEach((entry) => observer.observe(entry.tile));
  }
  refresh();
})();
