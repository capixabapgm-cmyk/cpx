import {cancelledIds,cancellationSchema} from './order-cancellations.mjs';
import {ApiError,numbers,publicRaffle} from './rules.mjs';
const idPattern=/^[a-f0-9-]{36}$/;
export async function manageOrder(request,env,row,target,action,data){
  if(row.closed)throw new ApiError('Rifa já sorteada. Não é possível alterar seus pedidos.',409);
  if(!Array.isArray(data.discard??[])||(data.discard??[]).length>100)throw new ApiError('Selecione até 100 pedidos por vez.');
  const discard=action==='delete'?[target.id]:[...new Set(data.discard??[])];
  if(discard.some(id=>typeof id!=='string'||!idPattern.test(id)||id===target.id&&action==='keep'))throw new ApiError('Pedidos inválidos.');
  if(action==='keep'&&!['pending','accepted'].includes(target.status))throw new ApiError('Este pedido não está ativo.',409);
  const all=(await env.DB.prepare('SELECT * FROM requests WHERE raffle_id=?').bind(row.id).all()).results;
  const map=new Map(all.map(order=>[order.id,order]));
  const selected=discard.map(id=>map.get(id));if(selected.some(order=>!order))throw new ApiError('Pedido não encontrado nesta rifa.',404);
  const targetNumbers=JSON.parse(target.numbers);
  if(action==='keep'&&selected.some(order=>!JSON.parse(order.numbers).some(n=>targetNumbers.includes(n))))throw new ApiError('Os pedidos escolhidos não têm números em comum.');
  const active=selected.filter(order=>['pending','accepted'].includes(order.status));
  // Keep cancelled rows as tombstones: stale devices cannot re-import or recreate them.
  if(!active.length&&(action==='delete'||target.status==='accepted'))return result(env,row.id);
  const expected=Number(request.headers.get('If-Match'));if(!request.headers.has('If-Match')||expected!==row.revision)throw new ApiError('Os pedidos mudaram. Atualize antes de continuar.',409);
  const preserve=new Set(numbers(data.preserve??[],row.total));
  const sold=new Set(JSON.parse(row.sold));
  const discarded=new Set(discard);
  const otherAccepted=new Set(all.filter(order=>order.status==='accepted'&&!discarded.has(order.id)).flatMap(order=>JSON.parse(order.numbers)));
  for(const order of active)if(order.status==='accepted')for(const n of JSON.parse(order.numbers))if(!otherAccepted.has(n)&&!preserve.has(n))sold.delete(n);
  if(action==='keep'&&target.status==='accepted'&&targetNumbers.some(n=>preserve.has(n)))throw new ApiError('Há venda manual ou de outro pedido nestes números. Nenhum pedido foi alterado.',409);
  if(action==='keep'&&target.status==='pending'){
    if(targetNumbers.some(n=>sold.has(n)))throw new ApiError('Há números vendidos por outro comprador. Nenhum pedido foi alterado.',409);
  }
  if(action==='keep')for(const n of targetNumbers)sold.add(n);
  const mutation=crypto.randomUUID(),updated=new Date().toISOString();
  const guards=[...active];if(action==='keep'&&!guards.some(order=>order.id===target.id))guards.push(target);
  const guardData=JSON.stringify(guards.map(order=>({id:order.id,status:order.status})));
  // Bulk SQL keeps parameter/query counts within the free D1 plan, even for 100 duplicates.
  const condition=" AND NOT EXISTS(SELECT 1 FROM json_each(?) expected WHERE NOT EXISTS(SELECT 1 FROM requests WHERE id=json_extract(expected.value,'$.id') AND raffle_id=? AND status=json_extract(expected.value,'$.status')))";
  const statements=[env.DB.prepare('UPDATE raffles SET sold=?,last_accept=?,updated_at=?,revision=revision+1 WHERE id=? AND revision=? AND closed=0'+condition).bind(JSON.stringify([...sold].sort((a,b)=>a-b)),mutation,updated,row.id,expected,guardData,row.id)];
  const activeIds=JSON.stringify(active.map(order=>order.id));
  if(active.length)statements.push(env.DB.prepare("UPDATE requests SET status='rejected' WHERE id IN(SELECT value FROM json_each(?)) AND raffle_id=? AND EXISTS(SELECT 1 FROM raffles WHERE id=? AND last_accept=?)").bind(activeIds,row.id,row.id,mutation));
  if(action==='keep')statements.push(env.DB.prepare("UPDATE requests SET status='accepted' WHERE id=? AND raffle_id=? AND EXISTS(SELECT 1 FROM raffles WHERE id=? AND last_accept=?)").bind(target.id,row.id,row.id,mutation));
  await env.DB.prepare(cancellationSchema).run();
  if(active.length)statements.push(env.DB.prepare('INSERT OR IGNORE INTO request_cancellations(request_id,raffle_id,cancelled_at) SELECT id,raffle_id,? FROM requests WHERE id IN(SELECT value FROM json_each(?)) AND raffle_id=? AND EXISTS(SELECT 1 FROM raffles WHERE id=? AND last_accept=?)').bind(updated,activeIds,row.id,row.id,mutation));
  const results=await env.DB.batch(statements);if(!results[0].meta.changes)throw new ApiError('Os pedidos mudaram. Atualize antes de continuar.',409);
  return result(env,row.id);
}
async function result(env,id){
  const row=await env.DB.prepare('SELECT * FROM raffles WHERE id=?').bind(id).first();
  const orders=await env.DB.prepare('SELECT * FROM requests WHERE raffle_id=? ORDER BY created_at DESC LIMIT 1200').bind(id).all();
  return {cancelled:await cancelledIds(env,id),raffle:publicRaffle(row),requests:orders.results.map(order=>({...order,numbers:JSON.parse(order.numbers)}))};
}
