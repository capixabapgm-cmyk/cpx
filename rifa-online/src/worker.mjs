import {ApiError,text,raffleData,requestData,publicRaffle} from './rules.mjs';
import {accountRoute,sessionUser} from './accounts.mjs';
const idPattern=/^[a-f0-9-]{36}$/;
const headers={'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'};
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers});
async function hash(value){return [...new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value)))].map(b=>b.toString(16).padStart(2,'0')).join('');}
async function body(request){
  if(Number(request.headers.get('content-length'))>300000)throw new ApiError('Arquivo muito grande.',413);
  const value=await request.text();if(value.length>300000)throw new ApiError('Arquivo muito grande.',413);
  try {const data=JSON.parse(value);if(!data||Array.isArray(data)||typeof data!=='object')throw 0;return data;}catch{throw new ApiError('Dados inválidos.');}
}
function token(request){const value=request.headers.get('Authorization')?.replace(/^Bearer /,'');if(!value||!/^[a-f0-9]{64}$/.test(value))throw new ApiError('Acesso não autorizado.',401);return value;}
async function owner(request,row,env){if(!row||await hash(token(request))!==row.owner_hash)throw new ApiError('Acesso não autorizado.',401);if(row.user_id){const user=await sessionUser(request,env);if(row.user_id!==user.id)throw new ApiError('Rifa de outra conta.',403);if(request.method==='PUT'){const backup=await env.DB.prepare('SELECT revision FROM backups WHERE user_id=?').bind(user.id).first();if(Number(request.headers.get('X-Backup-Revision'))!==(backup?.revision||0))throw new ApiError('Dados atualizados em outro aparelho. Sincronize a conta antes de publicar.',409);}}}
function requestRow(row){return {...row,numbers:JSON.parse(row.numbers)};}
export default {
  async fetch(request,env){
    try {
      const url=new URL(request.url);
      if(!url.pathname.startsWith('/api/'))return env.ASSETS.fetch(request);
      if(/^\/api\/(auth|account|admin)\//.test(url.pathname))return json(await accountRoute(request,env,url));
      if(url.pathname==='/api/health')return json({ok:true,service:'minhas-rifas',version:1});
      const match=url.pathname.match(/^\/api\/raffles\/([a-f0-9-]{36})(?:\/(requests)(?:\/([a-f0-9-]{36})\/(accept|reject))?)?$/);
      if(!match)throw new ApiError('Página não encontrada.',404);
      const [,id,requests,requestId,action]=match;
      if(!idPattern.test(id))throw new ApiError('Rifa inválida.');
      let row=await env.DB.prepare('SELECT * FROM raffles WHERE id=?').bind(id).first();
      if(!requests&&request.method==='PUT'){
        const credentials=token(request),data=raffleData(await body(request));
        if(!row){
          if(env.ADMIN_SETUP_KEY){const user=await sessionUser(request,env);await env.DB.prepare('INSERT INTO raffles(id,owner_hash,name,description,total,sold,closed,image,updated_at,user_id,pix_name,pix_key) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)').bind(id,await hash(credentials),data.name,data.description,data.total,JSON.stringify(data.sold),+data.closed,data.image,new Date().toISOString(),user.id,user.pix_name,user.pix_key).run();}
          else await env.DB.prepare('INSERT INTO raffles(id,owner_hash,name,description,total,sold,closed,image,updated_at) VALUES(?,?,?,?,?,?,?,?,?)').bind(id,await hash(credentials),data.name,data.description,data.total,JSON.stringify(data.sold),+data.closed,data.image,new Date().toISOString()).run();
        }else{
          await owner(request,row,env);
          // The device must acknowledge accepted orders before replacing availability.
          const expected=Number(request.headers.get('If-Match'));
          if(expected!==row.revision)throw new ApiError('A rifa mudou online. Atualize os pedidos antes de publicar novamente.',409);
          if(data.total!==row.total)throw new ApiError('A quantidade de números não pode mudar.',409);
          const result=await env.DB.prepare('UPDATE raffles SET name=?,description=?,sold=?,closed=?,image=?,revision=revision+1,updated_at=? WHERE id=? AND revision=?').bind(data.name,data.description,JSON.stringify(data.sold),+data.closed,data.image,new Date().toISOString(),id,expected).run();
          if(!result.meta.changes)throw new ApiError('A rifa mudou online. Sincronize novamente.',409);
          if(row.user_id){const user=await sessionUser(request,env);await env.DB.prepare('UPDATE raffles SET pix_name=?,pix_key=? WHERE id=? AND user_id=?').bind(user.pix_name,user.pix_key,id,user.id).run();}
        }
        row=await env.DB.prepare('SELECT * FROM raffles WHERE id=?').bind(id).first();return json(publicRaffle(row));
      }
      if(!row)throw new ApiError('Rifa não encontrada.',404);
      if(!requests&&request.method==='GET')return json(publicRaffle(row));
      if(requests&&!requestId&&request.method==='POST'){
        const input=await body(request);
        if(typeof input.id!=='string'||!idPattern.test(input.id))throw new ApiError('Identificador do pedido inválido.');
        const prior=await env.DB.prepare('SELECT * FROM requests WHERE id=?').bind(input.id).first();
        if(prior){if(prior.raffle_id!==id||prior.name!==input.name?.trim()||prior.phone!==input.phone?.trim()||prior.numbers!==JSON.stringify([...new Set(input.numbers??[])].sort((a,b)=>a-b)))throw new ApiError('Identificador já utilizado.',409);return json({id:prior.id,status:prior.status});}
        const data=requestData(input,row);
        // Rate cap per raffle bounds abuse without storing visitor addresses.
        const count=await env.DB.prepare("SELECT count(*) AS count FROM requests WHERE raffle_id=? AND created_at>?").bind(id,new Date(Date.now()-3600000).toISOString()).first();
        if(count.count>=200)throw new ApiError('Muitos pedidos nesta rifa. Tente mais tarde.',429);
        const pending=await env.DB.prepare("SELECT count(*) AS count FROM requests WHERE raffle_id=? AND status='pending'").bind(id).first();
        if(pending.count>=1000)throw new ApiError('A fila de pedidos está cheia. Entre em contato com o organizador.',429);
        await env.DB.prepare('INSERT INTO requests(id,raffle_id,name,phone,numbers,created_at) VALUES(?,?,?,?,?,?)').bind(input.id,id,data.name,data.phone,JSON.stringify(data.numbers),new Date().toISOString()).run();
        return json({id:input.id,status:'pending'},201);
      }
      await owner(request,row,env);
      if(requests&&!requestId&&request.method==='GET'){
        const rows=await env.DB.prepare('SELECT * FROM requests WHERE raffle_id=? ORDER BY created_at DESC LIMIT 1200').bind(id).all();
        return json({raffle:publicRaffle(row),requests:rows.results.map(requestRow)});
      }
      if(requestId&&request.method==='POST'){
        const order=await env.DB.prepare('SELECT * FROM requests WHERE id=? AND raffle_id=?').bind(requestId,id).first();if(!order)throw new ApiError('Pedido não encontrado.',404);
        if(order.status==='accepted'){if(action==='reject')throw new ApiError('Pedido já aceito.',409);return json({request:requestRow(order),raffle:publicRaffle(row)});}
        if(order.status!=='pending')throw new ApiError('Pedido já recusado.',409);
        if(action==='reject'){
          await env.DB.prepare("UPDATE requests SET status='rejected' WHERE id=? AND status='pending'").bind(requestId).run();
        }else{
          if(row.closed)throw new ApiError('Rifa encerrada.',409);
          const nums=JSON.parse(order.numbers);
          // D1 batch is transactional: availability and acceptance change together.
          await env.DB.batch([
            env.DB.prepare("UPDATE raffles SET sold=(SELECT json_group_array(value) FROM (SELECT value FROM json_each(sold) UNION SELECT value FROM json_each(?))), revision=revision+1,last_accept=?,updated_at=? WHERE id=? AND closed=0 AND NOT EXISTS(SELECT 1 FROM json_each(raffles.sold) occupied JOIN json_each(?) requested ON occupied.value=requested.value) AND EXISTS(SELECT 1 FROM requests WHERE id=? AND status='pending')").bind(order.numbers,requestId,new Date().toISOString(),id,order.numbers,requestId),
            env.DB.prepare("UPDATE requests SET status='accepted' WHERE id=? AND status='pending' AND EXISTS(SELECT 1 FROM raffles WHERE id=? AND last_accept=?)").bind(requestId,id,requestId)
          ]);
        }
        const updated=await env.DB.prepare('SELECT * FROM requests WHERE id=?').bind(requestId).first();
        if(action==='accept'&&updated.status!=='accepted')throw new ApiError('Algum número já foi vendido. Recuse o pedido ou combine outros números.',409);
        row=await env.DB.prepare('SELECT * FROM raffles WHERE id=?').bind(id).first();
        return json({request:requestRow(updated),raffle:publicRaffle(row)});
      }
      throw new ApiError('Método não permitido.',405);
    }catch(error){return json({error:error instanceof ApiError?error.message:'Não foi possível concluir. Tente novamente.'},error instanceof ApiError?error.status:500);}
  }
};
