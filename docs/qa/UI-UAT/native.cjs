const {chromium}=require(process.env.BASE_PLAYWRIGHT);
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const screens=require('./screens.cjs');
const [origin,scratch]=process.argv.slice(2);
assert.equal(new URL(origin).hostname,'127.0.0.1');
const supplemental=process.argv[4]==='payment';
const errors=process.argv[4]==='errors';
const log=path.join(__dirname,errors?'native-error.jsonl':supplemental?'native-payment.jsonl':'native.jsonl');
const size=p=>p.evaluate(()=>({width:innerWidth,outer:outerWidth,dpr:devicePixelRatio,scale:visualViewport.scale,scroll:document.documentElement.scrollWidth,client:document.documentElement.clientWidth}));
(async()=>{
  fs.writeFileSync(log,'');
  for(const role of supplemental?['Guest','Customer']:['Guest','Customer','Admin']){
    const ctx=await chromium.launchPersistentContext(path.join(scratch,'chrome-'+role),{executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',viewport:null,args:['--window-size=1672,941','--disable-background-networking']});
    try {
      await ctx.route(/^https?:/,r=>new URL(r.request().url()).origin===origin?r.continue():r.abort());
      const settings=await ctx.newPage();
      await settings.goto('chrome://settings/appearance');
      const page=await ctx.newPage();
      await page.goto(origin+'/login');
      const control=await size(page);
      const session=await ctx.newCDPSession(page);
      const version=(await session.send('Browser.getVersion')).product;
      if(role!=='Guest'){
        await page.locator('#username').fill((role==='Admin'?'admin':'buyer')+'@example.invalid');
        await page.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
        await Promise.all([page.waitForNavigation(),page.locator('form[action="/login"] button[type=submit]').click()]);
      }
      await settings.evaluate(()=>chrome.settingsPrivate.setDefaultZoom(2));
      const cases=errors?require('./error-screens.cjs'):supplemental?(role==='Guest'?[screens.find(s=>s.route==='/chat')]:
        [{route:'/payment/momo/mock/{id}',example:'/payment/momo/mock/301',status:200}]):screens.filter(s=>s.example && s.roles.includes(role));
      for(const s of cases){
        const row={role,route:s.route,js:true,control,version};
        try{
          const resp=await page.goto(origin+s.example);
          row.http=resp.status();row.measured=await size(page);
          const m=row.measured;
          assert.equal(m.outer,control.outer);
          assert.ok(Math.abs(m.dpr/control.dpr-2)<0.02 && Math.abs(m.width*2-control.width)<25 && m.scale===1);
          assert.equal(row.http,s.status);
          assert.equal(new URL(page.url()).pathname,s.example);
          assert.ok(await page.locator('main').isVisible());
          assert.ok(m.scroll<=m.client+1);
          row.status='PASS';
        }catch(e){row.status='FAIL';row.cause=e.name;}
        if((s.route==='/chat' && role==='Guest') || (row.status==='FAIL' && !fs.existsSync(path.join(__dirname,'../../ui/screenshots/UI-UAT/native-observation.png')))){
          row.image=row.status==='FAIL'?'native-observation.png':'native-200-chat.png';
          const redaction=await page.addStyleTag({content:'input,textarea,td,dd,.header-action {visibility:hidden !important}'});
          try {
            const frame=await session.send('Page.captureScreenshot',{format:'png',fromSurface:true,captureBeyondViewport:false});
            fs.writeFileSync(path.join(__dirname,'../../ui/screenshots/UI-UAT',row.image),Buffer.from(frame.data,'base64'));
          } finally {await redaction.evaluate(el=>el.remove());}
        }
        fs.appendFileSync(log,JSON.stringify(row)+'\n');
        if(row.status!=='PASS') process.exitCode=1;
      }
    } finally {await ctx.close();}
  }
  console.log('Chrome native page zoom checked using isolated profile per role');
})().catch(e=>{console.error(e.name+' during native setup');process.exitCode=1;});
