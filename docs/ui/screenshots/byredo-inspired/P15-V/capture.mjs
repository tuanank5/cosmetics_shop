import WebSocket from 'file:///C:/Users/phamm/AppData/Roaming/npm/node_modules/eas-cli/node_modules/ws/wrapper.mjs';
import { createServer } from 'node:http';
import { readFile, mkdir, writeFile, copyFile } from 'node:fs/promises';
import { spawn } from 'node:child_process';
import { resolve, extname } from 'node:path';
import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises';

const origin = 'http://127.0.0.1:' + (await readFile('target/p15-v/port','utf8')).trim();
const chrome = spawn('C:/Program Files/Google/Chrome/Application/chrome.exe', [
  '--headless=new', '--disable-gpu', '--no-sandbox', '--disable-background-networking', '--no-first-run',
  '--no-default-browser-check', '--remote-debugging-port=0', '--user-data-dir=' + resolve('target/p15-v/chrome-' + Date.now()), 'about:blank',
], { windowsHide: true, stdio: ['ignore', 'ignore', 'pipe'] });
let stderr = '';
chrome.stderr.on('data', b => { stderr += b; });
const finish = setTimeout(() => { console.error("Browser harness timed out"); chrome.kill();  process.exit(2); }, 90000);
const sockets = [];
try {
  let debuggerUrl;
  for (let i = 0; i < 100 && !debuggerUrl && chrome.exitCode === null; i++) {
    debuggerUrl = stderr.match(/DevTools listening on (ws:\/\/\S+)/)?.[1];
    if (!debuggerUrl) await delay(100);
  }
  if (!debuggerUrl) throw new Error('Chrome DevTools unavailable: ' + stderr.slice(0, 900));
  console.log("Chrome DevTools URL available");
  const cdpOrigin = new URL(debuggerUrl).origin.replace('ws:', 'http:');
  console.log("Requesting Chrome tabs");
  const tabs = await (await fetch(cdpOrigin + '/json/list')).json();
  const tab = tabs.find(t => t.type === 'page');
  console.log('Selected tab', tab?.url);
  console.log("Opening page WebSocket");
  const ws = new WebSocket(tab.webSocketDebuggerUrl, { perMessageDeflate: false });
  sockets.push(ws);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  console.log("Page WebSocket connected");
  let id = 0;
  const handlers = new Map();
  const events = [];
  ws.onmessage = ({data}) => {
    const m = JSON.parse(data);
    if (m.id) {
      const [res, rej] = handlers.get(m.id);
      handlers.delete(m.id);
      if (m.error) rej(new Error(m.error.message)); else res(m.result);
    } else {
      events.push(m);
    }
  };
  function cmd(method, params = {}) {
    return new Promise((resolve, reject) => {
      handlers.set(++id, [resolve, reject]);
      ws.send(JSON.stringify({ id, method, params }));
    });
  }
  const ev = async expression => {
    const r = await cmd('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.text);
    return r.result.value;
  };
  const viewport = (width, height) => cmd('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false });
  async function nav(path) {
    const expected = origin + path;
    await cmd('Page.navigate', { url: expected });
    for (let i = 0; i < 100; i++) {
      if (await ev('location.href + "|" + document.readyState') === expected + '|complete') return;
      await delay(50);
    }
    throw new Error('Document did not load: ' + path);
  }
  const key = async (name, code, modifiers = 0) => {
    const args = { key: name, code: name, windowsVirtualKeyCode: code, modifiers };
    await cmd('Input.dispatchKeyEvent', { ...args, type: 'keyDown', ...(name === 'Enter' ? { text: '\r', unmodifiedText: '\r' } : {}) });
    await cmd('Input.dispatchKeyEvent', { ...args, type: 'keyUp' });
  };
  console.log('Enabling page events');
  await cmd('Page.enable');
  console.log('Page events enabled');
  await cmd('Runtime.enable');
  await cmd('Log.enable');
  const out = resolve('docs/ui/screenshots/byredo-inspired/P15-V');
  const rows = [], references = [];
  const shot = async (name, full = false) => {
    const params = { format: 'png' };
    if (full) { const {cssContentSize:s} = await cmd('Page.getLayoutMetrics'); params.captureBeyondViewport = true; params.clip = {x:0,y:0,width:s.width,height:s.height,scale:1}; }
    await writeFile(resolve(out,name+'.png'),Buffer.from((await cmd('Page.captureScreenshot',params)).data,'base64'));
  };
  const inspect = () => ev(`(() => {
    const box = (s) => {const e=document.querySelector(s);if(!e)return null;const r=e.getBoundingClientRect(),c=getComputedStyle(e);return {x:r.x,y:r.y+scrollY,width:r.width,height:r.height,font:c.fontFamily,fontSize:c.fontSize,lineHeight:c.lineHeight,background:c.backgroundColor,border:c.border,borderRadius:c.borderRadius,columns:c.gridTemplateColumns,gap:c.gap};};
    return {url:location.pathname+location.search,title:document.title,width:document.documentElement.clientWidth,scrollWidth:document.documentElement.scrollWidth,height:document.documentElement.scrollHeight,dpr:devicePixelRatio,boxes:Object.fromEntries(['.site-header','.nav-shell','.header-search','.header-menu','.shop-hero','.shop-hero-copy','.shop-hero-art','h1','.catalog-layout','.catalog-filter-panel','.catalog-results','.store-grid','.store-card','.store-card-media','.pdp-layout','.pdp-media','.pdp-buy','.pdp-actions','.site-footer'].map(s=>[s,box(s)])),images:[...document.querySelectorAll('main img')].map(e=>({src:e.getAttribute('src'),naturalWidth:e.naturalWidth,naturalHeight:e.naturalHeight,hidden:e.hidden,objectFit:getComputedStyle(e).objectFit})),scripts:[...document.scripts].map(e=>e.getAttribute('src'))};})()`);
  await cmd('DOM.enable'); await cmd('CSS.enable');
  for(const [width,height] of [[1366,768],[390,844]]) {
    await viewport(width,height);
    for(const [name,path] of [['home','/'],['catalog','/products'],['pdp','/products/demo-r026?variant=22']]) {
      await nav(path); await delay(700);
      const row = await inspect(); row.viewport=[width,height]; row.zoom=100; row.fixture=true;
      const {root} = await cmd('DOM.getDocument');
      const {nodeId}=await cmd('DOM.querySelector',{nodeId:root.nodeId,selector:'h1'});
      row.headingFonts=(await cmd('CSS.getPlatformFontsForNode',{nodeId})).fonts;
      rows.push(row);
      await shot(`${name}-${width}-top`); await shot(`${name}-${width}-full`,true);
      await ev("document.querySelector('.site-footer').scrollIntoView()");await delay(80);await shot(`${name}-${width}-footer`);
      if(name==='catalog'){
        await ev("document.querySelector('.store-grid').scrollIntoView()");await delay(200);await shot(`catalog-${width}-grid`);
        if(width===390){await ev("document.querySelector('[data-catalog-filters]').open=true;document.querySelector('[data-catalog-filters]').scrollIntoView()");await shot(`catalog-${width}-filters-open`);}
      }
      if(name==='pdp'){await ev("document.querySelector('.pdp-buy').scrollIntoView()");await shot(`pdp-${width}-buy`);}
    }
  }
  await writeFile(resolve(out,'beautyshop-measurements.json'),JSON.stringify({browser:await ev('navigator.userAgent'),rows,jsExceptions:events.filter(e=>e.method==='Runtime.exceptionThrown').length},null,2));
  console.log('BeautyShop captures',rows.length);
  await cmd('Network.enable');
  for(const [width,height] of [[1366,768],[390,844]]) {
    await viewport(width,height);
    for(const [name,url] of [['home','https://www.byredo.com/us_en'],['catalog','https://www.byredo.com/us_en/c/holiday-selection'],['pdp','https://www.byredo.com/us_en/p/mojave-ghost-eau-de-parfum']]) {
      const start=events.length;
      const result=await cmd('Page.navigate',{url});await delay(1600);
      const row={name,url,viewport:[width,height],navigation:result,state:await ev('({url:location.href,title:document.title,text:document.body?.innerText?.slice(0,500)})'),failures:events.slice(start).filter(e=>e.method==='Network.loadingFailed').map(e=>e.params.errorText)};
      references.push(row);
      await shot(`reference-attempt-${name}-${width}`);
      console.log('Reference',name,width,result.errorText||row.state.title);
    }
  }
  await writeFile(resolve(out,'reference-access.json'),JSON.stringify(references,null,2));
} finally {clearTimeout(finish); for(const s of sockets)s.close(); chrome.kill();}
