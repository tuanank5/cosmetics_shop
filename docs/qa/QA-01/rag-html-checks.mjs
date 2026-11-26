  await blockApp(false);
  const question = '#cauHoi';
  const submit = 'form:has(#cauHoi) button[type=submit]';
  const allowed = 'Đổi trả trong bao lâu?';
  async function ask(value) {
    await go('/chat');
    await focus(question);
    await call('Input.insertText', { text: value });
    await focus(submit);
    await key('Enter', 13);
    await until(async () => assert.equal(await evaluate(
      'document.readyState === "complete" && document.querySelector("#answer-title")?.textContent'
    ), 'Câu trả lời'));
  }

  for (const [width, height] of [[390, 850], [768, 900], [1366, 950], [1920, 1080]]) {
    await viewport(width, height);
    for (const javascript of [true, false]) {
      await call('Network.clearBrowserCookies');
      await call('Emulation.setScriptExecutionDisabled', { value: !javascript });
      await ask(allowed);
      assert.equal(await evaluate('document.querySelector("#cauHoi").value'), allowed);
      assert.ok(await evaluate('document.querySelector("#answer-title").parentElement.textContent.includes("7 ngày")'));

      const rejected = javascript ? 'qa@example.invalid' : '000 000 0000';
      await ask(allowed + ' ' + rejected);
      assert.equal(await evaluate('document.querySelector("#cauHoi").value'), '');
      assert.ok(await evaluate('document.querySelector("#answer-title").parentElement.textContent.includes("Vui lòng bỏ dữ liệu cá nhân")'));
      assert.ok(await evaluate('!document.documentElement.outerHTML.includes(' + JSON.stringify(rejected) + ')'));
      assert.ok(await noOverflow());
      checks.push({ width, javascript, control: 'faq-retained', rejection: javascript ? 'email' : 'phone' });
    }
  }
  await writeFile('QA_RAG_EVIDENCE', JSON.stringify({ browser: Browser, checks }, null, 2) + '\n');
} finally {
  socket?.close();
  chrome.kill();
}
