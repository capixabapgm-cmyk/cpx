import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync} from 'node:fs';
import worker from '../src/worker.mjs';
const raffleId='12345678-1234-1234-1234-123456789012',token='a'.repeat(64);
const oid=n=>`00000000-0000-0000-0000-${String(n).padStart(12,'0')}`;
function environment(){
  const sqlite=new DatabaseSync(':memory:');sqlite.exec(readFileSync(new URL('../migrations/0001_initial.sql',import.meta.url),'utf8'));
  const DB={prepare(sql){let args=[];return {bind(...values){args=values;return this;},async first(){return sqlite.prepare(sql).get(...args)??null;},async all(){return {results:sqlite.prepare(sql).all(...args)};},async run(){return {meta:{changes:Number(sqlite.prepare(sql).run(...args).changes)}};}};},async batch(statements){sqlite.exec('BEGIN');try{const values=[];for(const statement of statements)values.push(await statement.run());sqlite.exec('COMMIT');return values;}catch(e){sqlite.exec('ROLLBACK');throw e;}}};
  return {DB,ASSETS:{fetch:()=>new Response('page')},sqlite};
}
async function call(env,path='',method='GET',body,auth=false,revision){
  const headers={};if(auth)headers.Authorization=`Bearer ${typeof auth==='string'?auth:token}`;if(revision!==undefined)headers['If-Match']=String(revision);
  const response=await worker.fetch(new Request(`https://test.invalid/api/raffles/${raffleId}${path}`,{method,headers,body:body?JSON.stringify(body):undefined}),env);
  return {status:response.status,data:await response.json()};
}
async function setup(){const env=environment();const result=await call(env,'','PUT',{name:'Rifa teste',description:'Prêmio',total:10,sold:[1],closed:false},true);assert.equal(result.status,200);return env;}
const order=(n,numbers)=>({id:oid(n),name:'Maria Silva',phone:'(85) 99999-1234',numbers});
test('Página pública mostra disponibilidade e não revela compradores ou credenciais',async()=>{
  const env=await setup();await call(env,'/requests','POST',order(1,[2,3]));
  const publicData=await call(env);assert.deepEqual(publicData.data.sold,[1]);assert.equal(publicData.data.owner_hash,undefined);assert.equal(publicData.data.phone,undefined);
  assert.equal((await call(env,'/requests')).status,401);assert.equal((await call(env,'/requests','GET',undefined,'b'.repeat(64))).status,401);
});
test('Pedido é pendente, não marca números como vendidos e pode ser recebido pelo dono',async()=>{
  const env=await setup();assert.equal((await call(env,'/requests','POST',order(1,[2,3]))).status,201);
  assert.deepEqual((await call(env)).data.sold,[1]);
  const list=await call(env,'/requests','GET',undefined,true);assert.equal(list.data.requests[0].status,'pending');assert.equal(list.data.requests[0].name,'Maria Silva');
});
test('Idempotência evita duplicar pedidos em reenvio, inclusive após aceitar',async()=>{
  const env=await setup();await call(env,'/requests','POST',order(1,[2,3]));
  assert.equal((await call(env,'/requests','POST',order(1,[2,3]))).status,200);
  await call(env,`/requests/${oid(1)}/accept`,'POST',{},true);
  const retry=await call(env,'/requests','POST',order(1,[2,3]));assert.equal(retry.status,200);assert.equal(retry.data.status,'accepted');
  assert.equal((await call(env,'/requests','GET',undefined,true)).data.requests.length,1);
});
test('Aprovação atualiza disponibilidade; pedido concorrente não sobrescreve números',async()=>{
  const env=await setup();await call(env,'/requests','POST',order(1,[2,3]));await call(env,'/requests','POST',order(2,[3,4]));
  assert.equal((await call(env,`/requests/${oid(1)}/accept`,'POST',{},true)).status,200);
  assert.equal((await call(env,`/requests/${oid(2)}/accept`,'POST',{},true)).status,409);
  assert.deepEqual((await call(env)).data.sold.sort((a,b)=>a-b),[1,2,3]);
  const list=await call(env,'/requests','GET',undefined,true);assert.equal(list.data.requests.find(r=>r.id===oid(2)).status,'pending');
});
test('Venda manual entre envio e aprovação impede aceitar o pedido',async()=>{
  const env=await setup();await call(env,'/requests','POST',order(1,[2,3]));
  assert.equal((await call(env,'','PUT',{name:'Rifa teste',description:'Prêmio',total:10,sold:[1,2],closed:false},true,1)).status,200);
  assert.equal((await call(env,`/requests/${oid(1)}/accept`,'POST',{},true)).status,409);
});
test('Recusar não altera disponibilidade e impede aceitar depois',async()=>{
  const env=await setup();await call(env,'/requests','POST',order(1,[2]));
  assert.equal((await call(env,`/requests/${oid(1)}/reject`,'POST',{},true)).data.request.status,'rejected');
  assert.deepEqual((await call(env)).data.sold,[1]);assert.equal((await call(env,`/requests/${oid(1)}/accept`,'POST',{},true)).status,409);
});
test('Validação impede números vendidos, fora da faixa, sem telefone e rifa encerrada',async()=>{
  const env=await setup();for(const data of [order(1,[1]),order(2,[11]),{...order(3,[2]),phone:''}])assert.ok((await call(env,'/requests','POST',data)).status>=400);
  await call(env,'','PUT',{name:'Rifa teste',description:'',total:10,sold:[1],closed:true},true,1);
  assert.equal((await call(env,'/requests','POST',order(4,[2]))).status,409);
});
test('Revisão impede sobrescrever uma aprovação com disponibilidade antiga',async()=>{
  const env=await setup();await call(env,'/requests','POST',order(1,[2]));await call(env,`/requests/${oid(1)}/accept`,'POST',{},true);
  assert.equal((await call(env,'','PUT',{name:'Rifa teste',description:'',total:10,sold:[1],closed:false},true,1)).status,409);
  assert.deepEqual((await call(env)).data.sold.sort((a,b)=>a-b),[1,2]);
});
