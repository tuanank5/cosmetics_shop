// P20 Dashboard: draws Chart.js charts from the data tables rendered by the server.
// The tables are the single source of truth (cells carry data-value); without JS or Chart.js they stay open.
(() => {
  const root = document.querySelector('.dashboard');
  if (!root) return;
  root.querySelectorAll('details[data-chart-table]').forEach((details) => {
    const key = 'beautyshop.dashboard.' + details.querySelector('table').id;
    try {
      if (sessionStorage.getItem(key) === 'closed') details.open = false;
      details.addEventListener('toggle', () => {
        if (details.open) sessionStorage.removeItem(key);
        else sessionStorage.setItem(key, 'closed');
      });
    } catch {
      // Keep the native table open when storage is unavailable.
    }
  });
  const Chart = window.Chart;
  const canvases = root.querySelectorAll('canvas[data-chart]');
  if (!Chart || canvases.length === 0) return;

  root.dataset.charts = 'true';
  const css = getComputedStyle(root);
  const token = (name, fallback) => css.getPropertyValue(name).trim() || fallback;
  const colors = { 1: token('--chart-1', '#9c6b1f'), 2: token('--chart-2', '#2a78d6') };
  const ink = token('--chart-ink', '#2a1f1d');
  const muted = token('--chart-muted', '#6b605a');
  const grid = token('--chart-grid', '#ebe5dd');
  const surface = token('--chart-surface', '#ffffff');
  const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

  const money = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 });
  const count = new Intl.NumberFormat('vi-VN');
  const compactMoney = new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 });
  const fmt = (format, value) => format === 'money' ? `${money.format(value)} ₫` : count.format(value);
  const tick = (format, value) => format === 'money' ? `${compactMoney.format(value)} ₫` : count.format(value);

  Chart.defaults.font.family = css.fontFamily;
  Chart.defaults.font.size = 12;
  Chart.defaults.color = muted;

  const readTable = (id, columns) => {
    const table = document.getElementById(id);
    if (!table) return null;
    const rows = [...table.tBodies[0].rows].filter((row) =>
      columns.every((column) => row.cells[column]?.dataset.value !== undefined));
    return {
      labels: rows.map((row) => row.cells[0].dataset.label || row.cells[0].textContent.trim()),
      shortLabels: rows.map((row) => row.cells[0].textContent.trim()),
      series: columns.map((column) => ({
        label: table.tHead.rows[0].cells[column].textContent.trim(),
        values: rows.map((row) => Number(row.cells[column].dataset.value)),
        labels: rows.map((row) => row.cells[column].dataset.label)
      }))
    };
  };

  const stripePattern = (context) => {
    const tile = document.createElement('canvas');
    tile.width = tile.height = 8;
    const brush = tile.getContext('2d');
    brush.fillStyle = surface;
    brush.fillRect(0, 0, 8, 8);
    brush.strokeStyle = colors[2];
    brush.lineWidth = 3;
    brush.beginPath();
    brush.moveTo(-2, 2);
    brush.lineTo(2, -2);
    brush.moveTo(-2, 10);
    brush.lineTo(10, -2);
    brush.moveTo(6, 10);
    brush.lineTo(10, 6);
    brush.stroke();
    return context.createPattern(tile, 'repeat');
  };

  canvases.forEach((canvas) => {
    const type = canvas.dataset.chart;
    const format = canvas.dataset.format;
    const stacked = type === 'stacked';
    const data = readTable(canvas.dataset.source, stacked ? [1, 2, 3] : [Number(canvas.dataset.column)]);
    if (!data || data.labels.length === 0) return;
    const color = colors[canvas.dataset.color] || colors[1];
    const horizontal = type === 'hbar';
    const isLine = type === 'line';
    const dense = data.labels.length > 31;
    const valueAxis = {
      beginAtZero: true,
      stacked,
      grid: { color: grid },
      border: { display: false },
      ticks: { color: muted, precision: format === 'count' ? 0 : undefined, callback: (value) => tick(format, value), maxTicksLimit: 5 }
    };
    const categoryAxis = {
      stacked,
      grid: { display: false },
      border: { color: grid },
      ticks: { color: horizontal ? ink : muted, autoSkip: !horizontal, maxRotation: 0, autoSkipPadding: 12 }
    };
    try {
      const fills = stacked ? [colors[1], stripePattern(canvas.getContext('2d')), colors[2]] : [color];
      new Chart(canvas, {
        type: isLine ? 'line' : 'bar',
        data: {
          labels: isLine || type === 'bar' ? data.shortLabels : data.labels,
          datasets: data.series.map((series, index) => ({
            label: stacked ? series.label : canvas.closest('.chart-card').querySelector('h2').textContent.trim(),
            data: series.values,
            borderColor: color,
            backgroundColor: fills[index],
            borderWidth: isLine ? 2 : 0,
            borderRadius: isLine ? 0 : 4,
            borderSkipped: 'start',
            maxBarThickness: stacked ? 72 : horizontal ? 22 : 18,
            pointRadius: isLine ? (dense ? 0 : 3) : 0,
            pointHoverRadius: 5,
            pointBackgroundColor: color,
            pointBorderColor: surface,
            pointBorderWidth: 2,
            tension: 0,
            fill: false
          }))
        },
        options: {
          indexAxis: horizontal ? 'y' : 'x',
          responsive: true,
          maintainAspectRatio: false,
          animation: reduceMotion ? false : { duration: 400 },
          interaction: isLine ? { mode: 'index', intersect: false } : { mode: 'nearest', intersect: true },
          plugins: {
            legend: { display: stacked, onClick: null },
            tooltip: {
              backgroundColor: ink,
              titleColor: surface,
              bodyColor: surface,
              displayColors: false,
              callbacks: {
                title: (items) => data.labels[items[0].dataIndex],
                label: (item) => {
                  const value = fmt(format, item.parsed[horizontal ? 'x' : 'y']);
                  return stacked ? `${data.series[item.datasetIndex].labels[item.dataIndex]}: ${value}` : value;
                }
              }
            }
          },
          scales: horizontal ? { x: valueAxis, y: categoryAxis } : { x: categoryAxis, y: valueAxis }
        }
      });
      canvas.dataset.chartReady = 'true';
    } catch (error) {
      canvas.closest('.chart-canvas')?.setAttribute('hidden', '');
      return;
    }
  });
})();
