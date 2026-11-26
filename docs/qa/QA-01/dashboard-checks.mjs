    const matrix = [];
    for (const js of [true, false]) {
      await call('Emulation.setScriptExecutionDisabled', { value: !js });
      for (const width of [390, 768, 1366, 1920]) {
        await viewport(width, 980);
        for (const days of [7, 30, 90]) {
          await go('/admin/dashboard?range=' + days + 'd&nguong=6');
          const data = await evaluate(`(() => {
            const money = text => Number(text.replace(/[^0-9]/g, ''));
            const kpi = Object.fromEntries(Array.from(document.querySelectorAll('[data-kpi]'), card => [
              card.dataset.kpi, money(card.querySelector('.kpi-value').textContent),
            ]));
            const daily = [...document.querySelector('#table-daily').tBodies[0].rows];
            const sum = col => daily.reduce((total, row) => total + Number(row.cells[col].dataset.value), 0);
            const charts = [...document.querySelectorAll('canvas[data-chart]')].map(canvas => {
              const columns = canvas.dataset.chart === 'stacked' ? [1, 2, 3] : [Number(canvas.dataset.column)];
              const rows = [...(document.getElementById(canvas.dataset.source)?.tBodies[0]?.rows ?? [])]
                .filter(row => columns.every(col => row.cells[col]?.dataset.value !== undefined));
              if (!rows.length) return null;
              const chart = window.Chart?.getChart(canvas);
              return chart?.data.datasets.every((set, i) => JSON.stringify(set.data)
                === JSON.stringify(rows.map(row => Number(row.cells[columns[i]].dataset.value)))) ?? false;
            }).filter(value => value !== null);
            return { kpi, revenue: sum(1), orders: sum(2), days: daily.length, charts,
              cancelRate: document.querySelector('[data-kpi="ti-le-huy"] .kpi-breakdown strong').textContent.trim(),
              tablesOpen: Array.from(document.querySelectorAll('[data-chart-table]')).every(table => table.open) };
          })()`);
          assert.deepEqual(data.kpi, {
            'doanh-thu': 125000, 'so-don': days === 7 ? 2 : 3, 'cho-xu-ly': 1, 'sku-sap-het': 2,
            'khach-moi': 1, aov: 125000, 'da-giao': 1, 'ti-le-huy': days === 7 ? 0 : 1,
          });
          assert.equal(data.cancelRate, days === 7 ? '0,0%' : '33,3%');
          assert.equal(data.days, days);
          assert.equal(data.revenue, data.kpi['doanh-thu']);
          assert.equal(data.orders, data.kpi['so-don']);
          assert.ok(await noOverflow());
          if (js) {
            assert.ok(data.charts.length >= 7 && data.charts.every(Boolean));
            if (width === 1920 && days === 90) await capture('dashboard-1920-90d');
          } else {
            assert.ok(data.tablesOpen && data.charts.every(value => !value));
          }
          matrix.push({ width, js, days, chartsMatch: js ? data.charts.length : 0, kpi: data.kpi });
        }
      }
    }
    await go('/admin/orders/2?nav=dashboard&range=30d');
    assert.ok(await evaluate('document.querySelector(\'main a[href="/admin/dashboard?range=30d"]\') !== null'));
    await go('/admin/returns/1?nav=dashboard&range=30d');
    assert.ok(await evaluate('document.querySelector(\'main a[href="/admin/dashboard?range=30d"]\') !== null'));
    assert.ok(await evaluate('document.querySelector(\'form[action="/admin/returns/1/status"] button[name=status]\') !== null'));
    await writeFile('docs/qa/QA-01/dashboard-matrix.json', JSON.stringify(matrix, null, 2) + '\n');
    checks.push('24-dashboard-combinations-eight-kpis-chart-table-and-redirect-period');
