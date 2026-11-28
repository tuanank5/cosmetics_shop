const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { webkit } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');
const [baseURL, output] = process.argv.slice(2);
assert.equal(baseURL, 'http://127.0.0.1:18516');

async function login(page, id) {
  await page.goto('/login');
  await page.locator('#username').fill('perf' + id + '@example.invalid');
  await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
  await Promise.all([page.waitForNavigation(),page.locator('form[action="/login"] button[type=submit]').click()]);
}

async function snapshot(page, quantity, lines) {
  const badge = Number(await page.locator('.cart-count').first().innerText());
  const values = await page.locator('[data-cart-line] input[type=number]').evaluateAll(inputs => inputs.map(i => Number(i.value)));
  assert.equal(values.length, lines);
  assert.equal(values.reduce((a,b) => a+b,0),badge);
  assert.equal(badge,quantity);
}

async function post(page, route, fields) {
  return page.evaluate(async ({route,fields}) => {
    const token = document.querySelector('input[name="_csrf"]').value;
    const res = await fetch(route,{method:'POST',body:new URLSearchParams({...fields,_csrf:token})});
    const doc = new DOMParser().parseFromString(await res.text(),'text/html');
    return {status:res.status, path:new URL(res.url).pathname,
      badge:doc.querySelector('.cart-count')?.textContent,
      valid:doc.querySelector('[data-quote-region]')?.dataset.quoteValid,
      discount:doc.querySelector('[data-checkout-discount]')?.textContent};
  },{route,fields});
}

(async () => {
  const browser = await webkit.launch();
  const passed = [];
  try {
    const guest = await browser.newContext({baseURL});
    const g = await guest.newPage();
    await g.goto('/cart');
    await snapshot(g,0,0);
    await g.goto('/products/perf-1');
    assert.equal((await post(g,'/cart/items',{variant:'1'})).path,'/cart');
    await g.goto('/cart');
    await snapshot(g,1,1);
    await g.goto('/checkout');
    assert.equal(new URL(g.url()).pathname,'/login');
    passed.push('anonymous empty, add and checkout login');
    await guest.close();

    const ctx = await browser.newContext({baseURL});
    const a = await ctx.newPage(), b = await ctx.newPage();
    await login(a,1);
    await Promise.all([a.goto('/cart'), b.goto('/cart')]);
    await snapshot(a,1,1);
    const add = page => post(page,'/cart/items',{variant:'1'});
    const replies = await Promise.all([add(a),add(b)]);
    for (const reply of replies) assert.equal(reply.path,'/cart');
    await Promise.all([a.reload(),b.reload()]);
    await snapshot(a,3,1);
    await snapshot(b,3,1);
    passed.push('two concurrent adds from two tabs, same SKU, both refreshed');
    await post(a,'/cart/items/1/quantity',{quantity:'4'});
    await snapshot(b,3,1);
    await b.reload();
    await snapshot(b,4,1);
    passed.push('quantity edit, old tab snapshot and reload');

    await a.goto('/checkout');
    const quote = await post(a,'/checkout/quote',{addressId:'1',voucher:'PFREVIEW'});
    assert.equal(quote.status,200);
    assert.equal(quote.valid,'true');
    assert.equal(quote.badge,'4');
    assert.match(quote.discount,/10[., ]000/);
    passed.push('valid voucher, total quantity four');
    await post(b,'/cart/items/1/quantity',{quantity:'0'});
    await b.reload();
    await snapshot(b,0,0);
    await a.goto('/checkout');
    assert.equal(await a.locator('.cart-count').first().innerText(),'0');
    assert.equal(await a.locator('[data-quote-region]').getAttribute('data-quote-valid'),'false');
    passed.push('remove last SKU, empty customer cart and checkout');
    await ctx.close();

    const stock = await browser.newContext({baseURL});
    const c = await stock.newPage();
    await login(c,3);
    await c.goto('/cart');
    await snapshot(c,10,10);
    assert.ok(await c.locator('#stock-3.cart-stock-warning').isVisible());
    await c.goto('/checkout');
    assert.equal(await c.locator('.cart-count').first().innerText(),'10');
    assert.equal(await c.locator('[data-quote-region]').getAttribute('data-quote-valid'),'false');
    passed.push('out-of-stock line counted, checkout quote invalid');
    await stock.close();
    fs.writeFileSync(path.join(output,'badge.json'),JSON.stringify({passed},null,2));
  } finally { await browser.close(); }
})().catch(error => {console.error(error.message);process.exitCode=1;});
