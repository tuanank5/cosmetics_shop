(() => {
  'use strict';
  const root = document.querySelector('[data-home-carousel]');
  const stage = document.getElementById('home-stage');
  const css = document.getElementById('home-carousel-css');
  if (!root || !stage || typeof window.Swiper !== 'function') return;
  const track = root.querySelector('.home-carousel-track');
  const slides = [...root.querySelectorAll('.home-slide')];
  const pagination = root.querySelector('.home-carousel-pagination');
  const header = stage.querySelector('.site-header');
  const menu = header?.querySelector('.header-menu');
  const discovery = header?.querySelector('.nav-discovery');
  if (!track || slides.length < 2 || !pagination || !menu) return;
  const sourcePagination = pagination.innerHTML;
  const sourceSlides = slides.map((slide) => ({ role: slide.getAttribute('role'), label: slide.getAttribute('aria-label') }));
  const desktop = matchMedia('(min-width: 1100px)');
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  const carouselViewport = matchMedia('(min-width: 901px) and (min-height: 560px) and (hover: hover) and (pointer: fine)');
  const traversal = performance.getEntriesByType('navigation')[0]?.type === 'back_forward';
  const opening = new URLSearchParams(location.search).get('home') === 'opening';
  const productAnchor = /^(home-featured|home-popular|home-all-products)-product-\d+$/;
  const sourceId = () => {
    const hash = location.hash.slice(1);
    if (productAnchor.test(hash)) return hash;
    const stored = history.state?.beautyshopHomeProduct;
    return traversal && productAnchor.test(stored ?? '') ? stored : null;
  };
  const sourceTarget = () => {
    const id = sourceId();
    if (!id) return null;
    const region = document.getElementById(id.split('-product-')[0]);
    return document.getElementById(id) || region?.querySelector('h2') || document.getElementById('home-title');
  };
  const restoreSource = () => {
    const target = sourceTarget();
    if (!target) return;
    const rect = target.getBoundingClientRect();
    if (rect.top < 0 || rect.top >= innerHeight || rect.bottom <= 0) {
      target.scrollIntoView({ block: 'start', behavior: 'instant' });
    }
    if (!target.matches('a[href], button, [tabindex]')) target.tabIndex = -1;
    if (document.activeElement !== target) target.focus({ preventScroll: true });
  };
  document.addEventListener('click', (event) => {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    const link = event.target.closest('a.store-card[id]');
    if (!link || !productAnchor.test(link.id) || link.target === '_blank') return;
    try {
      history.replaceState({ ...history.state, beautyshopHomeProduct: link.id }, '', location.href);
    } catch { /* The server Back link still has a stable anchor. */ }
  });
  let swiper, frame, initial = true;
  const notify = (enabled, activeIndex) => root.dispatchEvent(new CustomEvent('beautyshop:home-carousel', { detail: { enabled, activeIndex } }));
  const remember = (index) => {
    try {
      const state = history.state && typeof history.state === 'object' ? history.state : {};
      history.replaceState({ ...state, beautyshopHomeSlide: index }, '', location.href);
    } catch { /* Navigation remains usable when history persistence is unavailable. */ }
  };
  const sync = () => {
    if (!swiper || swiper.destroyed) return;
    const index = swiper.activeIndex;
    const hiddenFocus = slides.findIndex((slide) => slide.contains(document.activeElement));
    if (hiddenFocus >= 0 && hiddenFocus !== index) pagination.querySelectorAll('button')[index]?.focus({ preventScroll: true });
    slides.forEach((slide, i) => {
      slide.inert = i !== index;
      slide.setAttribute('aria-hidden', String(i !== index));
    });
    pagination.querySelectorAll('button').forEach((button, i) => {
      if (i === index) button.setAttribute('aria-current', 'true');
      else button.removeAttribute('aria-current');
    });
    root.dataset.activeSlide = String(index + 1);
    remember(index);
    notify(true, index);
  };
  const controls = () => {
    if (!swiper || swiper.destroyed) return;
    const rect = root.getBoundingClientRect();
    const interacting = discovery?.open || (!desktop.matches && menu.open)
      || header.querySelector('form:focus-within') !== null;
    const visible = rect.top >= -2 && rect.bottom <= innerHeight + 2 && !interacting && !document.hidden;
    if (visible) { swiper.mousewheel.enable(); swiper.keyboard.enable(); }
    else { swiper.mousewheel.disable(); swiper.keyboard.disable(); }
    swiper.allowTouchMove = !interacting;
  };
  const disable = (reason) => {
    root.dataset.carouselFallback = reason;
    if (!swiper && !root.classList.contains('is-ready')) return;
    const index = swiper?.activeIndex || 0;
    const wasInside = root.getBoundingClientRect().top >= -2;
    if (swiper && !swiper.destroyed) swiper.destroy(true, true);
    swiper = null;
    root.classList.remove('swiper', 'is-ready'); stage.classList.remove('is-ready');
    track.classList.remove('swiper-wrapper');
    pagination.innerHTML = sourcePagination;
    pagination.classList.remove('swiper-pagination-vertical', 'swiper-pagination-horizontal', 'swiper-pagination-clickable', 'swiper-pagination-bullets');
    slides.forEach((slide, i) => {
      slide.classList.remove('swiper-slide'); slide.inert = false; slide.removeAttribute('aria-hidden');
      for (const [name, value] of [['role', sourceSlides[i].role], ['aria-label', sourceSlides[i].label]]) {
        if (value === null) slide.removeAttribute(name); else slide.setAttribute(name, value);
      }
    });
    delete root.dataset.activeSlide;
    notify(false, index);
    if (wasInside) slides[index].scrollIntoView({ block: 'start' });
  };
  const eligible = () => !reduced.matches && carouselViewport.matches && !!css?.sheet;
  const enable = () => {
    if (!eligible()) { disable(reduced.matches ? 'reduced-motion' : 'viewport-or-css'); return; }
    if (swiper) {
      if (slides.some((slide) => slide.scrollHeight > root.clientHeight + 2)) disable('content-height');
      else controls();
      return;
    }
    let index = slides.reduce((best, slide, i) => Math.abs(slide.getBoundingClientRect().top) < Math.abs(slides[best].getBoundingClientRect().top) ? i : best, 0);
    const remembered = history.state?.beautyshopHomeSlide;
    const source = initial ? sourceTarget() : null;
    if (initial && opening && !traversal) index = 0;
    else if (source) {
      const slide = slides.findIndex((el) => el.contains(source));
      index = slide >= 0 ? slide : slides.length - 1;
    } else if (initial && traversal && Number.isInteger(remembered) && remembered >= 0 && remembered < slides.length) index = remembered;
    else if (initial && location.hash) { const target = slides.findIndex((slide) => '#' + slide.id === location.hash); if (target >= 0) index = target; }
    // P21: a hash below the carousel (#home-products, #home-all-products, #home-footer) keeps the reader there;
    // the carousel waits on the last chapter so scrolling back up returns to chapter 6.
    let below = null;
    if (!source && initial && location.hash && !slides.some((slide) => '#' + slide.id === location.hash)) {
      try { below = document.querySelector(location.hash); } catch { below = null; }
      if (below && (stage.contains(below) || !(stage.compareDocumentPosition(below) & Node.DOCUMENT_POSITION_FOLLOWING))) below = null;
      if (below) index = slides.length - 1;
    }
    const beforeY = scrollY;
    const preserveFooter = initial && traversal && beforeY > 2 && !location.hash;
    initial = false;
    root.classList.add('swiper', 'is-ready'); stage.classList.add('is-ready');
    track.classList.add('swiper-wrapper'); slides.forEach((slide) => slide.classList.add('swiper-slide'));
    try {
      swiper = new window.Swiper(root, {
        init: false, direction: 'vertical', slidesPerView: 1, loop: false, speed: 780,
        initialSlide: index, preventInteractionOnTransition: true,
        mousewheel: { enabled: true, eventsTarget: '#home-stage', forceToAxis: true, releaseOnEdges: true, thresholdTime: 0, thresholdDelta: 12 },
        keyboard: { enabled: true, onlyInViewport: true, pageUpDown: true },
        touchReleaseOnEdges: true, threshold: 12, grabCursor: false,
        pagination: { el: pagination, clickable: true, bulletElement: 'button', renderBullet: (i, className) => '<button type="button" class="' + className + '" aria-label="Chuyển đến chương ' + (i + 1) + '"></button>' },
        a11y: { enabled: true, scrollOnFocus: false, slideLabelMessage: 'Chương {{index}} / {{slidesLength}}', paginationBulletMessage: 'Chuyển đến chương {{index}}' }
      });
      swiper.on('init', sync); swiper.on('slideChange', sync);
      swiper.on('scroll', () => { swiper.params.mousewheel.thresholdTime = 800; });
      swiper.on('beforeTransitionStart', () => { swiper.params.mousewheel.releaseOnEdges = false; });
      swiper.on('transitionEnd', () => { swiper.params.mousewheel.releaseOnEdges = true; controls(); });
      swiper.init();
      if (slides.some((slide) => slide.scrollHeight > root.clientHeight + 2)) { disable('content-height'); return; }
      delete root.dataset.carouselFallback;
      if (source) { /* pageshow restores the source after native layout restoration. */ }
      else if (below) below.scrollIntoView({ block: 'start' });
      else scrollTo(0, preserveFooter ? beforeY : stage.offsetTop);
      sync(); controls();
    } catch { disable('initialization-error'); }
  };
  const schedule = () => { cancelAnimationFrame(frame); frame = requestAnimationFrame(enable); };
  menu.addEventListener('toggle', controls);
  discovery?.addEventListener('toggle', controls);
  header.addEventListener('focusin', controls);
  header.addEventListener('focusout', () => queueMicrotask(controls));
  stage.addEventListener('click', (event) => {
    if (!swiper) return;
    const link = event.target.closest('a[href^="#"]');
    const index = link && slides.findIndex((slide) => '#' + slide.id === link.getAttribute('href'));
    if (link && index >= 0) { event.preventDefault(); swiper.slideTo(index); }
  });
  window.addEventListener('scroll', controls, { passive: true });
  window.addEventListener('resize', schedule);
  reduced.addEventListener('change', schedule);
  carouselViewport.addEventListener('change', schedule);
  document.addEventListener('visibilitychange', controls);
  window.addEventListener('pagehide', () => { swiper?.mousewheel.disable(); swiper?.keyboard.disable(); });
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
      controls();
      return; // Browser restored the live document and its scroll position.
    }
    requestAnimationFrame(() => requestAnimationFrame(restoreSource));
  });
  css?.addEventListener('load', schedule);
  css?.addEventListener('error', () => disable('stylesheet-unavailable'));
  enable();
})();
