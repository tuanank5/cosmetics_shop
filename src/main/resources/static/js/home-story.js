(() => {
  'use strict';
  const stories = [...document.querySelectorAll('[data-story]')];
  if (!stories.length) return;
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const entries = [];
  let active, observer;
  const carousel = document.querySelector('[data-home-carousel]');
  let activeSlide = null;
  const selected = (entry) => !carousel?.classList.contains('is-ready') || stories.indexOf(entry.story) === activeSlide;
  const toggleVisible = (entry, visible) => {
    if (!visible && document.activeElement === entry.button) entry.story.querySelector('.story-spotlight a').focus();
    entry.button.hidden = !visible;
  };
  const permitted = (value) => {
    if (!value) return null;
    try {
      const url = new URL(value, location.href);
      return url.origin === location.origin && /^\/(img|uploads\/sku)\//.test(url.pathname) ? url.href : null;
    } catch { return null; }
  };
  const allowed = () => !reduced.matches && !document.hidden && !navigator.connection?.saveData;
  const stop = (entry) => {
    entry.generation++;
    entry.video.pause();
    entry.video.removeAttribute('src');
    entry.video.load();
    entry.story.classList.remove('is-video-playing');
    entry.button.textContent = 'Phát video';
    entry.button.setAttribute('aria-pressed', 'false');
    if (active === entry) active = null;
  };
  const play = async (entry) => {
    if (!allowed() || !selected(entry) || entry.failed || entry.paused || entry.ratio < .55 || active === entry) return;
    if (active) stop(active);
    active = entry;
    const generation = ++entry.generation;
    entry.video.muted = true;
    entry.video.src = entry.src;
    try {
      await entry.video.play();
      if (generation !== entry.generation || active !== entry || !allowed()) {
        if (active === entry) stop(entry);
        return;
      }
      entry.story.classList.add('is-video-playing');
      entry.button.textContent = 'Tạm dừng video';
      entry.button.setAttribute('aria-pressed', 'true');
    } catch {
      if (generation === entry.generation) { entry.failed = true; stop(entry); toggleVisible(entry, false); }
    }
  };
  stories.forEach((story) => {
    const video = story.querySelector('[data-story-video]');
    const button = story.querySelector('[data-story-toggle]');
    const src = video && permitted(video.dataset.videoSrc);
    if (!video || !button || !src) return;
    const entry = { story, video, button, src, ratio: 0, generation: 0, paused: false, failed: false };
    button.addEventListener('click', () => {
      entry.paused = active === entry;
      if (entry.paused) stop(entry); else play(entry);
    });
    video.addEventListener('error', () => {
      if (video.hasAttribute('src')) { entry.failed = true; stop(entry); toggleVisible(entry, false); }
    });
    entries.push(entry);
  });
  const refreshMedia = () => {
    if (active && (!allowed() || !selected(active))) stop(active);
    entries.forEach((entry) => { toggleVisible(entry, allowed() && !entry.failed); });
    if (allowed()) {
      const next = entries.filter((entry) => selected(entry) && entry.ratio >= .55 && !entry.paused && !entry.failed).sort((a,b) => b.ratio-a.ratio)[0];
      if (next) play(next);
    }
  };
  if ('IntersectionObserver' in window) {
    observer = new IntersectionObserver((records) => {
      records.forEach((record) => {
        const entry = entries.find((item) => item.story === record.target);
        if (entry) { entry.ratio = record.intersectionRatio; if (active === entry && entry.ratio < .55) stop(entry); }
      });
      refreshMedia();
    }, { threshold: [0,.55,.75,1] });
    entries.forEach((entry) => observer.observe(entry.story));
  }
  reduced.addEventListener('change', refreshMedia);
  carousel?.addEventListener('beautyshop:home-carousel', (event) => {
    activeSlide = event.detail.enabled ? event.detail.activeIndex : null;
    refreshMedia();
  });
  document.addEventListener('visibilitychange', refreshMedia);
  window.addEventListener('pagehide', () => { if (active) stop(active); });
  window.addEventListener('pageshow', (event) => { if (event.persisted) refreshMedia(); });
  refreshMedia();
})();
