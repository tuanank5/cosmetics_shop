  const follow = async selector => {
    const href = await evaluate(`document.querySelector(${JSON.stringify(selector)}).href`);
    await evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`);
    await until(async () => assert.equal(await evaluate('location.href + "|" + document.readyState'), href + '|complete'));
  };
  const homeState = () => evaluate(`(() => {
    const stage = document.querySelector('#home-stage').getBoundingClientRect();
    const chapter = document.querySelector('#home-featured').getBoundingClientRect();
    return {
      slide: document.querySelector('[data-home-carousel]').dataset.activeSlide,
      stageTop: stage.top, stageBottom: stage.bottom, chapterTop: chapter.top, chapterBottom: chapter.bottom,
      viewportHeight: innerHeight,
      focusInCard: Boolean(document.activeElement.closest('.store-card')),
    };
  })()`);

  await viewport(1920, 1080);
  await call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'no-preference' }] });
  await blockApp(false);
  await go('/');
  await until(async () => assert.equal((await homeState()).slide, '1'));
  await evaluate('document.querySelector(".home-carousel-pagination button:nth-child(5)").click()');
  await until(async () => {
    const state = await homeState();
    assert.equal(state.slide, '5');
    assert.ok(Math.abs(state.chapterBottom - state.stageBottom) <= 2, JSON.stringify(state));
  });
  await follow('#home-featured .store-card');
  assert.equal(await evaluate('new URL(document.querySelector(".pdp-back").href).hash'), '#home-featured');
  await follow('.pdp-back');
  const chapterReturn = await until(async () => {
    const state = await homeState();
    assert.equal(state.slide, '5');
    assert.ok(Math.abs(state.stageTop) <= 2 && Math.abs(state.chapterBottom - state.viewportHeight) <= 2, JSON.stringify(state));
    assert.equal(state.focusInCard, false);
    return state;
  });
  checks.push({ chapterReturn });

  await go('/#home-all-products');
  const lowerCard = '#home-all-products .store-card';
  const lowerId = await evaluate(`document.querySelector(${JSON.stringify(lowerCard)}).id`);
  await follow(lowerCard);
  await follow('.pdp-back');
  await until(async () => assert.equal(await evaluate('document.activeElement.id'), lowerId));
  checks.push('Home all-products still restores focus to the originating card.');

  await viewport(390, 850);
  await call('Emulation.setScriptExecutionDisabled', { value: true });
  await go('/#home-featured');
  await follow('#home-featured .store-card');
  await follow('.pdp-back');
  assert.equal(await evaluate('location.hash'), '#home-featured');
  assert.ok(await evaluate('document.querySelector("#featured-title").getBoundingClientRect().top < innerHeight'));
  checks.push('Home mobile with site JavaScript disabled: Chapter 5 heading remains visible after returning.');

  for (const width of [390, 1366]) {
    await viewport(width, 850);
    await call('Emulation.setScriptExecutionDisabled', { value: width === 390 });
    await go('/products?brand=fixture-brand&sort=priceDesc&page=1&size=1&filterPanel=open');
    const product = await evaluate('document.querySelector(".shop-catalog .store-card").id');
    assert.match(product, /^catalog-product-\d+$/);
    await follow('.shop-catalog .store-card');
    await follow('.pdp-back');
    const catalogReturn = await evaluate(`(() => {
      const card = document.getElementById(location.hash.slice(1));
      const bounds = card?.getBoundingClientRect();
      return {
        hash: location.hash, visible: Boolean(bounds && bounds.top < innerHeight && bounds.bottom > 0),
        params: Object.fromEntries(new URLSearchParams(location.search)),
      };
    })()`);
    assert.equal(catalogReturn.hash, '#' + product);
    assert.equal(catalogReturn.visible, true);
    for (const [key, value] of Object.entries({ brand: 'fixture-brand', sort: 'priceDesc', page: '1', size: '1', filterPanel: 'open' })) {
      assert.equal(catalogReturn.params[key], value);
    }
    checks.push({ width, catalogReturn });
    for (const route of ['/login', '/register']) {
      await go(route);
      const logo = await until(async () => {
        const metrics = await evaluate(`(() => {
          const wordmark = document.querySelector('.auth-intro img.auth-wordmark');
          return wordmark && { alt: wordmark.alt, intrinsicWidth: wordmark.naturalWidth, width: wordmark.getBoundingClientRect().width };
        })()`);
        assert.equal(metrics?.alt, 'BeautyShop');
        assert.ok(metrics.intrinsicWidth >= 800 && metrics.width > 0);
        return metrics;
      });
      assert.ok(await noOverflow());
      checks.push({ route, width, logo });
    }
  }
  await writeFile('docs/qa/NAV-AUTH-01/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
