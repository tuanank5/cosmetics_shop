  const pageText = (selector = 'main') =>
    evaluate('document.querySelector(' + JSON.stringify(selector) + ')?.innerText');
  const login = async email => {
    await go('/login');
    await focus('#username');
    await call('Input.insertText', { text: email });
    await focus('#password');
    await call('Input.insertText', { text: process.env.UI01C_FIXTURE_PASS });
    await key('Enter', 13);
  };
  const ready = async selector => until(async () => assert.ok(await evaluate(
    'document.readyState === "complete" && !!document.querySelector(' + JSON.stringify(selector) + ')',
  )));

  await viewport(390, 850);
  await go('/products/fixture-a');
  assert.match(await pageText(), /Mã biến thể sản phẩm: FIXTURE-A/);
  assert.ok(await noOverflow());
  await evaluate('document.querySelector(\'form[action="/cart/items"] button\').click()');
  await ready('[data-cart-line][data-sku="FIXTURE-A"]');
  assert.match(await pageText(), /Mã biến thể sản phẩm: FIXTURE-A/);
  await until(async () => assert.ok(await evaluate('!document.querySelector(\'[data-cart-step="1"]\').hidden')));
  await evaluate('document.querySelector(\'[data-cart-step="1"]\').click()');
  await until(async () => {
    const message = await evaluate('document.querySelector("[data-cart-live]").textContent');
    assert.match(message, /^Đã cập nhật số lượng cho mã biến thể sản phẩm FIXTURE-A: 2\./);
    assert.doesNotMatch(message, /SKU/);
  });
  assert.ok(await noOverflow());
  checks.push('Guest mobile: unchanged product code, cart update and live announcement');

  await login('buyer@example.invalid');
  await ready('form[action$="/logout"]');
  await go('/products/fixture-a');
  await until(async () => assert.equal(await evaluate(
    'document.querySelector("form[data-wishlist-id] button").getAttribute("aria-pressed")',
  ), 'false'));
  await evaluate(`(() => {
    const fetchReal = window.fetch;
    window.fetch = (url, options) => (
      options?.method === 'POST' && String(url).includes('wishlist')
        ? Promise.resolve(new Response(null, { status: 403 }))
        : fetchReal(url, options)
    );
    document.querySelector('form[data-wishlist-id] button').click();
  })()`);
  await until(async () => assert.equal(await evaluate(
    'document.querySelector("[data-wishlist-feedback]").textContent',
  ), 'Chưa thể xác nhận yêu cầu. Hãy tải lại trang và thử lại.'));
  assert.equal(await evaluate(
    'document.querySelector("form[data-wishlist-id] button").getAttribute("aria-pressed")',
  ), 'false');
  checks.push('Customer: simulated wishlist HTTP 403 gives the new message and preserves button state');

  await call('Network.clearBrowserCookies');
  await viewport(1366, 900);
  await login('admin@example.invalid');
  await ready('form[action$="/logout"]');
  const variants = '/admin/catalog/products/1/variants';
  await go(variants);
  assert.match(await pageText(), /Danh sách biến thể sản phẩm/);
  assert.match(await pageText(), /FIXTURE-A/);
  await go(variants + '/new');
  assert.match(await pageText(), /Thêm biến thể sản phẩm/);
  assert.match(await pageText(), /Mã biến thể sản phẩm/);
  await go(variants + '/1/edit');
  assert.match(await pageText(), /Chỉnh sửa biến thể sản phẩm/);
  assert.equal(await evaluate('document.querySelector("[name=sku]").value'), 'FIXTURE-A');
  assert.ok(await noOverflow());
  checks.push('Admin desktop: list, new/edit variant labels and unchanged stored code');
  await writeFile('docs/qa/UI-COPY-01/browser-results.json', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
