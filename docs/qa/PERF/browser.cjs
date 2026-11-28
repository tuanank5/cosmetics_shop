const { webkit, chromium } = require(process.env.BASE_PLAYWRIGHT || 'playwright-core');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const [baseURL, directory] = process.argv.slice(2);
assert.equal(baseURL, 'http://127.0.0.1:18516');

async function measure(engine, name, options = {}) {
  const browser = await engine.launch({ ...options, proxy: {server:'http://127.0.0.1:1', bypass:'127.0.0.1,localhost'} });
  const rows = [];
  try {
    for (const width of [390, 1440]) {
      for (const route of ['/', '/products', '/products/perf-1']) {
        const context = await browser.newContext({baseURL, viewport:{width,height:900}});
        const page = await context.newPage();
        await page.addInitScript(() => {
          const supported = PerformanceObserver.supportedEntryTypes;
          window.vitals = {lcp:null, cls:null};
          if (supported.includes('largest-contentful-paint')) {
            new PerformanceObserver(list => {
              window.vitals.lcp = list.getEntries().at(-1).startTime;
            }).observe({type:'largest-contentful-paint', buffered:true});
          }
          if (supported.includes('layout-shift')) {
            window.vitals.cls = 0;
            let start = 0, previous = 0, sum = 0;
            new PerformanceObserver(list => {
              for (const e of list.getEntries()) {
                if (e.hadRecentInput) continue;
                if (e.startTime-previous >= 1000 || e.startTime-start >= 5000) {
                  start = e.startTime; sum = 0;
                }
                previous = e.startTime;
                sum += e.value;
                window.vitals.cls = Math.max(window.vitals.cls, sum);
              }
            }).observe({type:'layout-shift', buffered:true});
          }
        });
        for (const visit of ['cold','warm']) {
          const response = await page.goto(route);
          assert.equal(response.status(),200);
          await page.waitForLoadState('networkidle');
          const metrics = await page.evaluate(() => ({
            ...window.vitals,
            images: Array.from(document.images, i => ({src:i.getAttribute('src'),loading:i.loading,
              priority:i.fetchPriority, width:i.width,height:i.height,below:i.getBoundingClientRect().top>=innerHeight})),
            waterfall:performance.getEntriesByType('resource').filter(r=>r.name.startsWith(location.origin))
              .map(r=>({path:new URL(r.name).pathname,transfer:r.transferSize,body:r.encodedBodySize,
                start:r.startTime,ms:r.duration}))
          }));
          rows.push({name,width,route,visit,...metrics});
        }
        if (route === '/products/perf-1') {
          const src = await page.locator('[data-sku-image]').getAttribute('src');
          const first = await context.request.get(src);
          const headers = first.headers();
          const second = await context.request.get(src, {headers:headers.etag ? {'If-None-Match':headers.etag} :
            {'If-Modified-Since':headers['last-modified'] || new Date().toUTCString()}});
          rows.at(-1).skuCache = {url:src, first:first.status(), conditional:second.status(),
            bytes:(await first.body()).length, cacheControl:headers['cache-control'], etag:headers.etag || null};
        }
        await context.close();
      }
    }
    fs.writeFileSync(path.join(directory,name+'.json'), JSON.stringify(rows,null,2));
  } finally {await browser.close();}
}
(async () => {
  await measure(webkit,'webkit');
  const edge = process.env['ProgramFiles(x86)'] + '/Microsoft/Edge/Application/msedge.exe';
  if (fs.existsSync(edge)) await measure(chromium,'edge',{executablePath:edge});
})().catch(e=>{console.error(e.message);process.exitCode=1;});
