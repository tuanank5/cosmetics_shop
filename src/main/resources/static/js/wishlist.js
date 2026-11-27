(() => {
  'use strict';
  const filter = document.querySelector('[data-catalog-filters]');
  filter?.addEventListener('toggle', () => {
    document.querySelectorAll('[data-wishlist-origin="catalog"] input[name="filterPanel"]').forEach(input => {
      input.value = filter.open ? 'open' : 'closed';
    });
  });

  const forms = new Map();
  document.querySelectorAll('form[data-wishlist-id]').forEach(form => {
    const id = form.dataset.wishlistId;
    if (!forms.has(id)) forms.set(id, []);
    forms.get(id).push(form);
  });
  const page = document.querySelector('[data-wishlist-page]');
  const statePath = page?.dataset.state || forms.values().next().value?.[0].dataset.state;
  if (!statePath) return;

  const feedback = document.querySelector('[data-wishlist-feedback]');
  const count = page?.querySelector('[data-wishlist-count]');
  const busy = new Set();
  let generation = 0;
  let hetPhien = false;
  const khongXacNhanYeuCau = new Error('Chưa thể xác nhận yêu cầu. Hãy tải lại trang và thử lại.');
  const channel = typeof BroadcastChannel === 'function' ? new BroadcastChannel('beautyshop-wishlist') : null;
  const noi = message => { if (feedback) feedback.textContent = message; };

  function capNhat(id, saved) {
    for (const form of forms.get(id) || []) {
      const button = form.querySelector('button');
      const label = saved ? 'Bỏ yêu thích' : 'Lưu yêu thích';
      form.action = saved ? form.dataset.remove : form.dataset.add;
      button.setAttribute('aria-pressed', String(saved));
      button.setAttribute('aria-label', label + ' ' + form.dataset.wishlistName);
      button.title = label;
      button.querySelector('[data-wishlist-label]').textContent = label;
      const row = form.closest('[data-wishlist-row]');
      if (row && !saved) {
        const canFocus = row.contains(document.activeElement);
        const next = row.nextElementSibling || row.previousElementSibling;
        row.remove();
        forms.delete(id);
        if (canFocus) (next?.querySelector('[data-wishlist-id] button') || page.querySelector('[data-wishlist-heading]')).focus();
        page.querySelector('[data-wishlist-empty]').hidden = forms.size !== 0;
        if (count) count.textContent = forms.size + ' sản phẩm';
      }
    }
  }

  async function docJson(response) {
    if (response.redirected || response.status === 401) {
      hetPhien = true;
      throw new Error('Bạn cần đăng nhập lại để tiếp tục.');
    }
    if (response.status === 403) throw khongXacNhanYeuCau;
    if (!response.headers.get('Content-Type')?.startsWith('application/json') || (!response.ok && response.status !== 409)) {
      return null;
    }
    return response.json();
  }

  async function docTrangThai(ids) {
    const url = new URL(statePath, location.href);
    if (ids) url.searchParams.set('ids', ids.join(','));
    const result = await docJson(await fetch(url, {
      cache: 'no-store', credentials: 'same-origin', headers: { Accept: 'application/json' },
      signal: AbortSignal.timeout?.(8000),
    }));
    if (!Array.isArray(result)) throw new Error('Chưa đọc được trạng thái yêu thích.');
    return new Set(result);
  }

  async function lamMoi() {
    if (hetPhien || document.hidden || busy.size) return;
    const luot = ++generation;
    const ids = [...forms.keys()];
    try {
      if (page) {
        const saved = await docTrangThai();
        if (luot !== generation) return;
        if ([...saved].some(id => !forms.has(id))) {
          location.reload();
          return;
        }
        ids.forEach(id => capNhat(id, saved.has(id)));
        return;
      }
      for (let start = 0; start < ids.length; start += 48) {
        const batch = ids.slice(start, start + 48);
        const saved = await docTrangThai(batch);
        if (luot !== generation) return;
        batch.forEach(id => capNhat(id, saved.has(id)));
      }
    } catch (error) {
      if (hetPhien && luot === generation) noi(error.message);
    }
  }

  function cho(id, dangCho) {
    for (const form of forms.get(id) || []) {
      const button = form.querySelector('button');
      if (dangCho) button.setAttribute('aria-disabled', 'true');
      else button.removeAttribute('aria-disabled');
    }
  }

  document.addEventListener('submit', async event => {
    const form = event.target;
    const id = form.dataset.wishlistId;
    if (!id || new URL(form.action).origin !== location.origin) return;
    event.preventDefault();
    event.stopPropagation();
    if (busy.has(id)) return;
    if (hetPhien) {
      noi('Bạn cần đăng nhập lại để tiếp tục.');
      return;
    }
    ++generation;
    busy.add(id);
    cho(id, true);
    noi('');
    let canDocLai = false;
    try {
      const response = await fetch(form.action, {
        method: 'POST', credentials: 'same-origin', cache: 'no-store',
        body: new URLSearchParams(new FormData(form)),
        headers: { Accept: 'application/json', 'X-Wishlist-Partial': '1' },
        signal: AbortSignal.timeout?.(8000),
      });
      const result = await docJson(response);
      if (typeof result?.saved === 'boolean') {
        capNhat(id, result.saved);
        noi(result.message || '');
        channel?.postMessage('refresh');
      } else {
        canDocLai = true;
      }
    } catch (error) {
      if (hetPhien || error === khongXacNhanYeuCau) noi(error.message);
      else canDocLai = true;
    } finally {
      if (canDocLai) {
        try {
          const saved = await docTrangThai([id]);
          capNhat(id, saved.has(id));
          noi('Kết nối bị gián đoạn. Đã đọc lại trạng thái yêu thích.');
          channel?.postMessage('refresh');
        } catch (error) {
          noi(hetPhien ? error.message : 'Chưa xác nhận được trạng thái yêu thích. Hãy tải lại trang trước khi thử lại.');
        }
      }
      ++generation;
      busy.delete(id);
      cho(id, false);
    }
  }, true);

  window.addEventListener('focus', lamMoi);
  window.addEventListener('pageshow', event => { if (event.persisted) lamMoi(); });
  document.addEventListener('visibilitychange', lamMoi);
  if (channel) channel.addEventListener('message', lamMoi);
})();
