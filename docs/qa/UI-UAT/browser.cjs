const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {webkit} = require(process.env.BASE_PLAYWRIGHT);
const errorFixture=process.argv[3]==='errors';
const screens = errorFixture?require('./error-screens.cjs'):require('./screens.cjs');
const origin = process.argv[2];
assert.equal(new URL(origin).hostname,'127.0.0.1');
const out = __dirname;
const pictures = path.resolve(out,'../../ui/screenshots/UI-UAT');
fs.mkdirSync(pictures,{recursive:true});
const log = path.join(out,errorFixture?'error-browser.jsonl':'browser.jsonl');
fs.writeFileSync(log,'');
const reported = new Set();
const sizes = [[1672,941],[768,1024],[390,844]];
async function capture(page,file) {
  const mask = page.locator('input,textarea,select,td,dd,.header-action,.customer-info,[href^="mailto:"],a[href^="tel:"]');
  await page.screenshot({path:path.join(pictures,file),mask:[mask],timeout:6000,animations:'disabled'});
}
async function run(browser,role,width,height,js) {
  const ctx = await browser.newContext({baseURL:origin,viewport:{width,height},javaScriptEnabled:js});
  await ctx.route('**/*',r=>new URL(r.request().url()).origin===origin?r.continue():r.abort());
  const page=await ctx.newPage();
  page.setDefaultTimeout(5000);
  try {
    if(role!=='Guest') {
      await page.goto('/login');
      await page.locator('#username').fill((role==='Admin'?'admin':'buyer')+'@example.invalid');
      await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
      await Promise.all([page.waitForNavigation(),page.locator('form[action="/login"] button[type=submit]').click()]);
      assert.notEqual(new URL(page.url()).pathname,'/login');
    }
    for(const s of screens.filter(s=>s.example && s.roles.includes(role))) {
      const row={route:s.route,role,width,height,js,state:s.state};
      try {
        const response=await page.goto(s.example,{waitUntil:'domcontentloaded'});
        await page.locator('main').first().waitFor({state:'visible'});
        row.http=response.status();
        row.correctPath=new URL(page.url()).pathname===s.example;
        row.mainCount=await page.locator('main').count();
        row.layout=await page.evaluate(()=>({client:document.documentElement.clientWidth,scroll:document.documentElement.scrollWidth}));
        row.render=row.http===s.status && row.correctPath && row.mainCount===1?'PASS':'FAIL';
        row.reflow=row.layout.scroll<=row.layout.client+1?'PASS':'FAIL';
        row.skip='NOT RUN';
        if(row.render==='PASS'){
          const skip=page.locator('a[href="#main"]').first();
          if(await skip.count()) {
            await skip.focus();
            await page.keyboard.press('Enter');
            row.skip=await page.evaluate(()=>document.activeElement.id==='main')?'PASS':'FAIL';
          } else row.skip='FAIL';
        }
        if(s.state==='long') row.data=await page.locator('main').innerText().then(t=>t.includes('LongNameFixture')?'PASS':'FAIL');
        if(s.state==='empty') row.data=await page.locator('main').innerText().then(t=>/chưa|trống|không có/i.test(t)?'PASS':'FAIL');
        if(s.state==='error') row.data=row.http===s.status?'PASS':'FAIL';
        row.status=[row.render,row.reflow,row.skip,row.data].includes('FAIL')?'FAIL':'PASS';
        const bad=s.template+'-'+[row.render,row.reflow,row.skip,row.data].join('-');
        const sample=role==='Guest' && width===390 && !js && s.route==='/chat';
        if(sample || (row.status==='FAIL' && !reported.has(bad) && reported.size<8)){
          const file=sample?'guest-mobile-chat.png':'observation-'+(reported.size+1)+'.png';
          if(!sample)reported.add(bad);
          await capture(page,file);row.image=file;
        }
      } catch(e) {
        row.status='BLOCKED';row.cause=e.name;
      }
      fs.appendFileSync(log,JSON.stringify(row)+'\n');
      if(row.status!=='PASS') process.exitCode=1;
    }
  } finally {await ctx.close();}
}
(async()=>{
  const browser=await webkit.launch();
  try {
    for(const [width,height] of sizes) for(const js of [true,false])
      for(const role of ['Guest','Customer','Admin']) await run(browser,role,width,height,js);
    console.log('Matrix completed on WebKit '+browser.version());
  } finally {await browser.close();}
})().catch(e=>{console.error(e.name+' during matrix setup');process.exitCode=1;});
