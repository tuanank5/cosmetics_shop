import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import vm from 'node:vm';

const base = 'target/biz-ux-02/baseline/src/main/resources/static';
const current = 'src/main/resources/static';
const read = (root, file) => readFile(path.join(root, file), 'utf8');

const cases = [
  ...[320, 360, 390, 768, 1024, 1280, 1440, 1920].map(width => ({
    name: width + ' CSS px', width, height: 844, coarse: width <= 768,
  })),
  { name: 'short desktop', width: 1440, height: 540 },
  { name: 'wide touch', width: 1280, height: 844, coarse: true },
  { name: 'wide reduced motion', width: 1440, height: 844, reduce: true },
  { name: 'stylesheet unavailable', width: 1440, height: 844, noCss: true },
];

function eligible(source, scenario) {
  const body = source.match(/const eligible = \(\) => (.+);/)[1];
  const query = source.match(/const carouselViewport = matchMedia\('([^']+)'\)/)?.[1];
  const mediaMatches = !query || query.split(' and ').every(condition => {
    const feature = condition.match(/\(min-(width|height): (\d+)px\)/);
    if (feature) return scenario[feature[1]] >= +feature[2];
    assert.ok(['(hover: hover)', '(pointer: fine)'].includes(condition));
    return !scenario.coarse;
  });
  return vm.runInNewContext(body, {
    innerWidth: scenario.width, innerHeight: scenario.height,
    carouselViewport: { matches: mediaMatches }, reduced: { matches: !!scenario.reduce },
    css: { sheet: scenario.noCss ? null : {} },
  });
}
const beforeJs = await read(base, 'js/home-carousel.js');
const afterJs = await read(current, 'js/home-carousel.js');
const carousel = cases.map(scenario => {
  const before = eligible(beforeJs, scenario);
  const after = eligible(afterJs, scenario);
  assert.equal(after, scenario.width >= 901 && scenario.height >= 560 && !scenario.coarse && !scenario.reduce && !scenario.noCss);
  return { ...scenario, before, after };
});
assert.equal(carousel[2].before, true);
assert.equal(carousel[2].after, false);

function luminance(hex) {
  return [0.2126, 0.7152, 0.0722].reduce((sum, weight, i) => {
    const s = parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16) / 255;
    return sum + weight * (s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4);
  }, 0);
}
function contrast(a, b) {
  const sorted = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return Math.round((sorted[0] + 0.05) / (sorted[1] + 0.05) * 100) / 100;
}

const paletteBefore = await read(base, 'css/admin.css');
const paletteAfter = await read(current, 'css/admin.css');
const tokens = await read(current, 'css/tokens.css');
const selected = paletteBefore.match(/--dash-gold: (#[0-9a-f]{6})/i)[1];
const originalRing = tokens.match(/--color-primary: (#[0-9a-f]{6})/i)[1];
assert.match(paletteAfter, /\.dashboard-range a\[aria-current=page\]:focus-visible \{ outline-color: currentColor; \}/);
assert.match(paletteAfter, /\.dashboard-range a\[aria-current=page\] \{ background: var\(--dash-gold\); color: #fff;/);
const focus = { foregroundBefore: originalRing, foregroundAfter: '#ffffff', adjacent: selected,
  beforeRatio: contrast(originalRing, selected), afterRatio: contrast('#ffffff', selected) };
assert.ok(focus.beforeRatio < 3);
assert.ok(focus.afterRatio >= 3);
await writeFile('docs/qa/BIZ-UX-02/source-check.json', JSON.stringify({
  evidence: 'actual JS predicate + author CSS colors; no browser layout or accessibility tree',
  carousel, dashboardSelectedFocus: focus,
}, null, 2) + '\n');
console.log('PASS: ' + carousel.length + ' eligibility cases; focus ' + focus.beforeRatio + ' -> ' + focus.afterRatio);
