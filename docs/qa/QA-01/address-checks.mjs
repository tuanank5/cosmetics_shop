  await viewport(390, 850);
  await go('/account/addresses/new?from=checkout');
  const fill = async (field, value) => {
    await evaluate(`(() => { const field = document.getElementById(${JSON.stringify(field)}); field.focus(); field.select(); })()`);
    await call('Input.insertText', { text: value });
  };
  await fill('tenNguoiNhan', 'Fixture Receiver');
  await fill('soDienThoai', '0900000000');
  await fill('diaChiChiTiet', 'Fixture second address');
  const save = async () => {
    await evaluate('document.querySelector("main form button[type=submit]").focus()');
    await key('Enter', 13);
    await until(async () => assert.equal(await evaluate('location.pathname + "|" + document.readyState'), '/checkout|complete'));
  };
  await save();
  assert.ok(await evaluate('document.querySelector("#addressId").textContent.includes("Fixture second address")'));
  await go('/account/addresses?from=checkout');
  const edit = await evaluate(`Array.from(document.querySelectorAll('.catalog-list li'))
    .find(row => row.innerText.includes('Fixture second address')).querySelector('a').href`);
  await go(edit.slice(base.length));
  await fill('diaChiChiTiet', 'Fixture edited address');
  await save();
  assert.ok(await evaluate('document.querySelector("#addressId").textContent.includes("Fixture edited address")'));
  assert.ok(await evaluate('!document.querySelector("#addressId").textContent.includes("Fixture second address")'));
  checks.push('no-js-create-and-edit-address-return-to-checkout');

  await go('/orders/1');
  assert.ok(await evaluate('document.querySelector("main").innerText.includes("Fixture order snapshot")'));
  assert.ok(await evaluate('document.querySelector(\'main a[href="/orders"]\') !== null'));
  await go('/account/returns/1');
  assert.ok(await evaluate('document.querySelector("main").innerText.includes("Fixture return")'));
  assert.ok(await evaluate('document.querySelector(\'main a[href="/account/returns"]\') !== null'));
  for (const route of ['/orders/2', '/account/returns/2']) {
    await go(route);
    assert.ok(await evaluate('document.title.includes("Không tìm thấy trang")'));
  }
  checks.push('mobile-no-js-order-and-return-details-and-non-owner-404');
