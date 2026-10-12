import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {environment} from './api.test.mjs';
import worker from '../src/worker.mjs';
const id='12345678-1234-1234-1234-123456789012';
function setup(image='data:image/jpeg;base64,/9j/2Q=='){
  const env=environment();env.ASSETS.fetch=()=>new Response(readFileSync(new URL('../public/index.html',import.meta.url),'utf8'),{headers:{'Content-Type':'text/html','Content-Security-Policy':"script-src 'self'",ETag:'static', 'Cache-Control':'public, max-age=3600'}});
  env.sqlite.prepare('INSERT INTO raffles(id,owner_hash,name,description,total,sold,image,updated_at) VALUES(?,?,?,?,?,?,?,?)').run(id,'PRIVATE-OWNER-KEY','Prêmio "Rosa" <script>','Descrição <b>especial</b>',10,'[]',image,new Date().toISOString());return env;
}
const get=(env,path='/?r='+id,options)=>worker.fetch(new Request('https://rifas.example'+path,options),env);
test('Prévia existe no HTML inicial, escapa conteúdo e preserva a página e CSP',async()=>{
  const env=setup();const response=await get(env);assert.equal(response.status,200);const html=await response.text();
  assert.match(html,/<meta property="og:title" content="Prêmio &quot;Rosa&quot; &lt;script&gt;">/);
  assert.match(html,/<meta property="og:description" content="Descrição &lt;b&gt;especial&lt;\/b&gt;">/);
  assert.match(html,new RegExp('https://rifas.example/api/raffles/'+id+'/image\\?v=[a-f0-9]{16}'));
  assert.match(html,/id="order"/);assert.match(html,/src="\/app.js"/);assert.ok(!html.includes('PRIVATE-OWNER-KEY'));
  assert.equal(response.headers.get('Content-Security-Policy'),"script-src 'self'");assert.equal(response.headers.get('Cache-Control'),'no-store');assert.equal(response.headers.get('ETag'),null);
  assert.ok(html.indexOf('property="og:image"')<html.indexOf('</head>'));
});
test('Imagem da prévia é JPEG público leve, permite HEAD/cache e não expõe credenciais',async()=>{
  const env=setup();const response=await get(env,'/api/raffles/'+id+'/image');assert.equal(response.status,200);assert.equal(response.headers.get('Content-Type'),'image/jpeg');assert.deepEqual([...new Uint8Array(await response.arrayBuffer())],[255,216,255,217]);
  const etag=response.headers.get('ETag');assert.equal((await get(env,'/api/raffles/'+id+'/image',{headers:{'If-None-Match':etag}})).status,304);
  assert.equal(await (await get(env,'/api/raffles/'+id+'/image',{method:'HEAD'})).text(),'');
  assert.equal((await get(env,'/api/raffles/'+id+'/image',{method:'POST'})).status,405);
});
test('Mudança da foto renova URL; mudança só do nome mantém imagem em cache',async()=>{
  const env=setup();const image=html=>html.match(/property="og:image" content="([^"]+)"/)[1];const first=image(await (await get(env)).text());
  env.sqlite.prepare('UPDATE raffles SET name=? WHERE id=?').run('Nome novo',id);assert.equal(image(await (await get(env)).text()),first);
  env.sqlite.prepare('UPDATE raffles SET image=? WHERE id=?').run('data:image/jpeg;base64,/9j/AA==',id);assert.notEqual(image(await (await get(env)).text()),first);
});
test('Rifa sem foto usa prévia textual; IDs inexistentes não retornam imagem',async()=>{
  const env=setup('');const html=await (await get(env)).text();assert.match(html,/property="og:title"/);assert.ok(!html.includes('property="og:image"'));assert.equal((await get(env,'/api/raffles/'+id+'/image')).status,404);
  assert.equal((await get(env,'/api/raffles/00000000-0000-0000-0000-000000000000/image')).status,404);
});

test('Texto com tokens de substituição permanece literal no HTML',async()=>{
  const env=setup();env.sqlite.prepare('UPDATE raffles SET name=? WHERE id=?').run('$& $` $\' ',id);
  const html=await (await get(env)).text();assert.ok(html.includes('content="$&amp; $` $&#39; "'));assert.equal((html.match(/<head>/g)||[]).length,1);
});

test('Assets com redirecionamento de index.html não perdem ID nem metadados',async()=>{
  const env=setup();const serve=env.ASSETS.fetch;env.ASSETS.fetch=request=>new URL(request.url).pathname==='/index.html'?new Response(null,{status:307,headers:{Location:'/'}}):serve(request);
  const response=await get(env,'/?r='+id+'&preview=foto2');assert.equal(response.status,200);assert.equal(response.headers.get('Location'),null);
  const html=await response.text();assert.match(html,/property="og:image"/);assert.ok(html.includes('/?r='+id+'&amp;preview=foto2'));
});
