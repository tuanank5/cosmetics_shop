const {webkit}=require(process.env.BASE_PLAYWRIGHT);
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const baseURL=process.argv[2];assert.equal(new URL(baseURL).hostname,'127.0.0.1');
const log=path.join(__dirname,'payment-screen.jsonl');fs.writeFileSync(log,'');
(async()=>{
 const browser=await webkit.launch();
 try{
  for(const [width,height] of [[1672,941],[768,1024],[390,844],[320,640]]) for(const js of [true,false]){
   const ctx=await browser.newContext({baseURL,viewport:{width,height},javaScriptEnabled:js});
   await ctx.route('**/*',r=>new URL(r.request().url()).origin===baseURL?r.continue():r.abort());
   const p=await ctx.newPage(),row={role:'Customer',route:'/payment/momo/mock/{id}',width,height,js,state:'default'};
   try{
    await p.goto('/login');await p.locator('#username').fill('buyer@example.invalid');
    await p.locator('#password').fill(process.env.CART01_FIXTURE_PASS);
    await Promise.all([p.waitForNavigation(),p.locator('form[action="/login"] button[type=submit]').click()]);
    row.http=(await p.goto('/payment/momo/mock/301')).status();
    assert.equal(row.http,200);assert.match(await p.locator('h1').innerText(),/MÔ PHỎNG - KHÔNG THU TIỀN/);
    assert.equal(await p.locator('main form button').count(),3);row.render='PASS';
    row.layout=await p.evaluate(()=>({client:document.documentElement.clientWidth,scroll:document.documentElement.scrollWidth}));
    row.reflow=row.layout.scroll<=row.layout.client+1?'PASS':'FAIL';
    await p.locator('a[href="#main"]').first().focus();await p.keyboard.press('Enter');
    row.skip=await p.evaluate(()=>document.activeElement.id==='main')?'PASS':'FAIL';
    row.status=row.skip==='PASS'&&row.reflow==='PASS'?'PASS':'FAIL';
   }catch(e){row.status='FAIL';row.cause=e.name;}
   fs.appendFileSync(log,JSON.stringify(row)+'\n');
   if(row.status!=='PASS') process.exitCode=1;
   await ctx.close();
  }
 }finally{await browser.close();}
})().catch(e=>{console.error(e.name);process.exitCode=1;});
