(() => {
  'use strict';

  const gsap = window.gsap;
  const art = document.querySelector('.shop-hero-art[aria-hidden="true"]');
  if (!art || !gsap || gsap.version !== '3.15.1' || typeof gsap.matchMedia !== 'function') return;

  const parts = ['.shop-hero-arch', '.shop-hero-vessel', '.shop-hero-jar']
    .map((selector) => art.querySelector(selector));
  if (parts.some((part) => !part || part.matches('a, button, input, select, textarea, [tabindex]'))) return;

  let motion;
  const start = () => {
    if (motion) return;
    motion = gsap.matchMedia();
    motion.add('(prefers-reduced-motion: no-preference)', () => {
      gsap.from(parts, {
        y: 6,
        opacity: 0.86,
        duration: 0.26,
        stagger: 0.06,
        ease: 'power1.out',
        force3D: false,
        // Restore original styles after the 380 ms entrance as well as on preference changes.
        onComplete() { this.revert(); }
      });
    });
  };

  // Let the existing header disclosure finish layout before moving the decoration.
  let frame;
  const schedule = () => {
    window.cancelAnimationFrame(frame);
    frame = window.requestAnimationFrame(() => {
      frame = window.requestAnimationFrame(start);
    });
  };
  schedule();
  window.addEventListener('pagehide', () => {
    window.cancelAnimationFrame(frame);
    if (motion) motion.revert();
    motion = null;
  });
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) schedule();
  });
})();
