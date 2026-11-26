  await blockApp(false);
  await call('Runtime.enable');
  await call('Runtime.addBinding', { name: 'recordSkinAdviceSubmit' });
  let loading;
  socket.addEventListener('message', event => {
    const { method, params } = JSON.parse(event.data);
    if (method === 'Runtime.bindingCalled' && params.name === 'recordSkinAdviceSubmit') {
      loading = JSON.parse(params.payload);
    }
  });

  const submit = '#skin-advice-form + form button[type=submit]';
  async function fill(value) {
    await go('/tu-van-da');
    assert.equal(await evaluate('document.querySelector(".skip-link").hash'), '#main');
    assert.ok(await evaluate('document.querySelector("main#main") !== null'));
    assert.equal(await evaluate('document.querySelector("#vanDeDa").maxLength'), 1000);
    for (const name of ['loaiDa', 'daNhayCam', 'nhuCau']) {
      await focus('#' + name);
      await key('ArrowDown', 40);
    }
    await focus('#vanDeDa');
    await call('Input.insertText', { text: value });
    await focus(submit);
  }

  for (const [width, height] of [[390, 850], [768, 900], [1366, 950], [1920, 1080]]) {
    await viewport(width, height);
    for (const javascript of [true, false]) {
      await call('Emulation.setScriptExecutionDisabled', { value: !javascript });
      await fill('Tẩy tế bào chết');
      const before = await call('Page.getNavigationHistory');
      loading = undefined;
      await evaluate(`document.querySelector('#vanDeDa').form.addEventListener('submit', event => {
        recordSkinAdviceSubmit(JSON.stringify({
          busy: event.currentTarget.getAttribute('aria-busy'),
          loading: event.submitter.classList.contains('is-loading'),
          disabled: event.submitter.disabled
        }));
      }, { once: true })`);
      await key('Enter', 13);
      await until(async () => assert.ok(await evaluate(
        'document.readyState === "complete" && document.querySelector("#skin-advice-result").parentElement.textContent.includes("chưa tìm thấy")'
      )));
      if (javascript) {
        assert.deepEqual(loading, { busy: 'true', loading: true, disabled: false });
      }
      assert.ok(await noOverflow());
      if (width === 390) {
        await call('Page.navigateToHistoryEntry', { entryId: before.entries[before.currentIndex].id });
        await until(async () => assert.ok(await evaluate(
          `document.readyState === 'complete' && !document.querySelector('${submit}').classList.contains('is-loading')
            && !document.querySelector('${submit}').disabled`
        )));
      }
      checks.push({ width, javascript, retainedResponse: 'catalog-empty', back: width === 390 });
    }
  }

  await viewport(1366, 950);
  await call('Emulation.setScriptExecutionDisabled', { value: false });
  await fill('Chăm sóc da cơ bản');
  await key('Enter', 13);
  await until(async () => assert.ok(await evaluate(
    'document.readyState === "complete" && document.querySelector(".auth-panel [role=alert]")?.textContent.includes("AI tư vấn hiện chưa sẵn sàng")'
  )));
  await capture('service-error-1366');
  await viewport(390, 850);
  await go('/tu-van-da');
  await capture('form-390');
  await writeFile('docs/qa/SKIN-UI-01/browser-results.json', JSON.stringify({ browser: Browser, checks, providerDisabled: 'alert' }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
