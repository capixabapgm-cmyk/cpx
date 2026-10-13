import {cancelledIds} from './order-cancellations.mjs';
import {accountTheme,saveTheme} from './themes.mjs';
import {ApiError,text} from './rules.mjs';
export const hash=async s=>[...new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(s)))].map(b=>b.toString(16).padStart(2,'0')).join('');
const random=()=>[...crypto.getRandomValues(new Uint8Array(32))].map(b=>b.toString(16).padStart(2,'0')).join('');
const now=()=>new Date().toISOString();
function email(value){const s=text(value,'E-mail',254).toLowerCase();if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s))throw new ApiError('E-mail inválido.');return s;}
async function input(request,max=1800000){const value=await request.text();if(value.length>max)throw new ApiError('Dados muito grandes.',413);try{return JSON.parse(value);}catch{throw new ApiError('Dados inválidos.');}}
async function passwordHash(password,salt,secret){
  if(typeof password!=='string'||password.length<12||password.length>128)throw new ApiError('Use uma senha com 12 a 128 caracteres.');
  const key=await crypto.subtle.importKey('raw',new TextEncoder().encode(password),'PBKDF2',false,['deriveBits']);
  const bits=await crypto.subtle.deriveBits({name:'PBKDF2',hash:'SHA-256',salt:new TextEncoder().encode(salt),iterations:100000},key,256);
  return hash([...new Uint8Array(bits)].join(',')+secret);
}
const safeUser=u=>({id:u.id,email:u.email,name:u.name,role:u.role,blocked:!!u.blocked,pixName:u.pix_name,pixKey:u.pix_key});
async function rate(request,env,label){
  const key=await hash((request.headers.get('CF-Connecting-IP')||'unknown')+label+env.ADMIN_SETUP_KEY),window=Math.floor(Date.now()/900000);
  await env.DB.prepare('INSERT INTO auth_rate(key,window,count) VALUES(?,?,1) ON CONFLICT(key) DO UPDATE SET count=CASE WHEN auth_rate.window=excluded.window THEN auth_rate.count+1 ELSE 1 END,window=excluded.window').bind(key,window).run();
  const row=await env.DB.prepare('SELECT count FROM auth_rate WHERE key=?').bind(key).first();if(row.count>20)throw new ApiError('Muitas tentativas. Aguarde 15 minutos.',429);
}
export async function sessionUser(request,env){
  const token=request.headers.get('X-Session-Token')||request.headers.get('Authorization')?.replace(/^Bearer /,'');if(!token||!/^[a-f0-9]{64}$/.test(token))throw new ApiError('Entre na sua conta.',401);
  const user=await env.DB.prepare('SELECT users.* FROM sessions JOIN users ON users.id=sessions.user_id WHERE sessions.token_hash=? AND sessions.expires_at>?').bind(await hash(token),now()).first();
  if(!user||user.blocked)throw new ApiError('Sessão expirada ou conta bloqueada. Entre novamente.',401);return user;
}
async function loginSession(env,user){const token=random();await env.DB.prepare('INSERT INTO sessions(token_hash,user_id,expires_at) VALUES(?,?,?)').bind(await hash(token),user.id,new Date(Date.now()+30*86400000).toISOString()).run();return {user:{...safeUser(user),theme:await accountTheme(env,user.id)},token};}
export async function accountRoute(request,env,url){
  if(!env.ADMIN_SETUP_KEY||env.ADMIN_SETUP_KEY.length<32)throw new ApiError('Login ainda não ativado. O administrador precisa configurar o serviço.',503);
  const path=url.pathname;
  if(path==='/api/auth/setup'&&request.method==='POST'){
    await rate(request,env,'setup');const data=await input(request,10000);
    if(typeof data.setupKey!=='string'||await hash(data.setupKey)!==await hash(env.ADMIN_SETUP_KEY))throw new ApiError('Código de ativação inválido.',401);
    if(await env.DB.prepare("SELECT id FROM users WHERE role='admin'").first())throw new ApiError('Administrador já cadastrado. Use Entrar.',409);
    const id=crypto.randomUUID(),salt=random(),address=email(data.email),name=text(data.name,'Nome',120),digest=await passwordHash(data.password,salt,env.ADMIN_SETUP_KEY);
    if(!env.ADMIN_EMAIL||address!==env.ADMIN_EMAIL.toLowerCase())throw new ApiError('Este e-mail não é o administrador configurado.',403);
    await env.DB.prepare("INSERT INTO users(id,email,name,password_hash,salt,role,pix_name,pix_key,created_at) VALUES(?,?,?,?,?,'admin',?,?,?)").bind(id,address,name,digest,salt,'Francisco Claudemir da Silva Andrade','78216834268',now()).run();
    return loginSession(env,await env.DB.prepare('SELECT * FROM users WHERE id=?').bind(id).first());
  }
  if(path==='/api/auth/register'&&request.method==='POST'){
    await rate(request,env,'register');const data=await input(request,10000),address=email(data.email),name=text(data.name,'Nome',120),code=text(data.invite,'Convite',128).replace(/\s/g,'');
    const invite=await env.DB.prepare('SELECT * FROM invites WHERE code_hash=? AND revoked=0 AND used_by IS NULL AND expires_at>?').bind(await hash(code),now()).first();
    if(!invite||invite.email&&invite.email!==address)throw new ApiError('Convite inválido, expirado ou já utilizado.');
    if(await env.DB.prepare('SELECT id FROM users WHERE email=?').bind(address).first())throw new ApiError('Já existe uma conta com esse e-mail. Use Entrar.',409);
    const id=crypto.randomUUID(),salt=random(),digest=await passwordHash(data.password,salt,env.ADMIN_SETUP_KEY);
    await env.DB.batch([
      env.DB.prepare("INSERT INTO users(id,email,name,password_hash,salt,created_at) SELECT ?,?,?,?,?,? WHERE EXISTS(SELECT 1 FROM invites WHERE id=? AND used_by IS NULL AND revoked=0 AND expires_at>?)").bind(id,address,name,digest,salt,now(),invite.id,now()),
      env.DB.prepare('UPDATE invites SET used_by=? WHERE id=? AND used_by IS NULL AND EXISTS(SELECT 1 FROM users WHERE id=?)').bind(id,invite.id,id)
    ]);
    const user=await env.DB.prepare('SELECT * FROM users WHERE id=?').bind(id).first();if(!user)throw new ApiError('Convite já utilizado.',409);return loginSession(env,user);
  }
  if(path==='/api/auth/login'&&request.method==='POST'){
    await rate(request,env,'login');const data=await input(request,10000),address=email(data.email),user=await env.DB.prepare('SELECT * FROM users WHERE email=?').bind(address).first();
    const digest=await passwordHash(data.password,user?.salt||'dummy-salt-not-a-user',env.ADMIN_SETUP_KEY);
    if(!user||digest!==user.password_hash||user.blocked)throw new ApiError('E-mail ou senha inválidos, ou conta bloqueada.',401);return loginSession(env,user);
  }
  const user=await sessionUser(request,env);
  if(path==='/api/auth/me'&&request.method==='GET')return {user:{...safeUser(user),theme:await accountTheme(env,user.id)}};
  if(path==='/api/auth/logout'&&request.method==='POST'){const token=request.headers.get('Authorization')?.replace(/^Bearer /,'');await env.DB.prepare('DELETE FROM sessions WHERE token_hash=?').bind(await hash(token)).run();return {ok:true};}
  if(path==='/api/account/theme'){
    if(request.method==='PUT'){const data=await input(request,10000);await saveTheme(env,user.id,data.theme);}
    else if(request.method!=='GET')throw new ApiError('Método não permitido.',405);
    return {user:{...safeUser(user),theme:await accountTheme(env,user.id)}};
  }
  if(path==='/api/account/pix'&&request.method==='PUT'){
    const data=await input(request,10000),name=text(data.name,'Destinatário',120,false),key=text(data.key,'Chave Pix',160,false);
    if(key&&!name)throw new ApiError('Informe o destinatário do Pix.');await env.DB.prepare('UPDATE users SET pix_name=?,pix_key=? WHERE id=?').bind(name,key,user.id).run();return {user:{...safeUser(user),pixName:name,pixKey:key,theme:await accountTheme(env,user.id)}};
  }
  if(path==='/api/account/backup'){
    if(request.method==='GET'){const row=await env.DB.prepare('SELECT * FROM backups WHERE user_id=?').bind(user.id).first();return {revision:row?.revision||0,raffles:row?JSON.parse(row.data):[]};}
    if(request.method==='PUT'){
      const data=await input(request);if(!Array.isArray(data.raffles)||data.raffles.length>100||!Number.isInteger(data.revision)||data.revision<0)throw new ApiError('Dados de sincronização inválidos.');
      const owned=[];
      for(const r of data.raffles){
        if(!r||typeof r!=='object'||typeof r.accountId!=='string'||r.accountId!==user.id||typeof r.id!=='string'||!/^[a-f0-9-]{36}$/.test(r.id)||!Number.isInteger(r.total)||r.total<1||r.total>10000||!r.sales||typeof r.sales!=='object'||Array.isArray(r.sales))throw new ApiError('Rifa inválida.');
        text(r.name,'Nome da rifa',200);text(r.description??'','Descrição',4000,false);
        if(r.imageData&&(typeof r.imageData!=='string'||r.imageData.length>250000||!/^data:image\/jpeg;base64,[A-Za-z0-9+/=]+$/.test(r.imageData)))throw new ApiError('Imagem inválida.');
        for(const [n,sale] of Object.entries(r.sales)){if(!/^\d+$/.test(n)||+n<1||+n>r.total||!sale||typeof sale!=='object')throw new ApiError('Venda inválida.');text(sale.name,'Comprador',200);text(sale.phone??'','Telefone',80,false);if(sale.paid!==undefined&&typeof sale.paid!=='boolean')throw new ApiError('Pagamento inválido.');}
        if(r.webId){const online=await env.DB.prepare('SELECT * FROM raffles WHERE id=?').bind(r.webId).first();if(online){if(online.user_id&&online.user_id!==user.id)throw new ApiError('Rifa de outra conta.',403);if(!r.webToken||await hash(r.webToken)!==online.owner_hash)throw new ApiError('Acesso à rifa inválido.',403);const cancelled=new Set(await cancelledIds(env,r.webId));if(Object.values(r.sales).some(sale=>cancelled.has(sale.requestId)))throw new ApiError('Atualize os pedidos antes de salvar: há pedidos excluídos neste backup.',409);owned.push(r.webId);}}
      }
      const existing=await env.DB.prepare('SELECT revision FROM backups WHERE user_id=?').bind(user.id).first();if((existing?.revision||0)!==data.revision)throw new ApiError('Dados alterados em outro aparelho. Atualize antes de salvar.',409);
      const serialized=JSON.stringify(data.raffles),updated=now();
      if(existing){const changed=await env.DB.prepare('UPDATE backups SET data=?,revision=revision+1,updated_at=? WHERE user_id=? AND revision=?').bind(serialized,updated,user.id,data.revision).run();if(!changed.meta.changes)throw new ApiError('Conflito de sincronização.',409);}
      else await env.DB.prepare('INSERT INTO backups(user_id,data,updated_at) VALUES(?,?,?)').bind(user.id,serialized,updated).run();
      for(const id of owned)await env.DB.prepare('UPDATE raffles SET user_id=? WHERE id=? AND (user_id IS NULL OR user_id=?)').bind(user.id,id,user.id).run();
      return {revision:data.revision+1};
    }
  }
  if(!path.startsWith('/api/admin/'))throw new ApiError('Página não encontrada.',404);
  if(user.role!=='admin')throw new ApiError('Acesso restrito ao administrador.',403);
  if(path==='/api/admin/users'&&request.method==='GET'){const users=await env.DB.prepare('SELECT * FROM users ORDER BY created_at DESC LIMIT 500').all();return {users:users.results.map(safeUser)};}
  if(path==='/api/admin/users/block'&&request.method==='POST'){const data=await input(request,10000);if(data.id===user.id||typeof data.blocked!=='boolean')throw new ApiError('Operação inválida.');await env.DB.prepare("UPDATE users SET blocked=? WHERE id=? AND role!='admin'").bind(+data.blocked,data.id).run();return {ok:true};}
  if(path==='/api/admin/invites'){
    if(request.method==='GET'){const rows=await env.DB.prepare('SELECT id,email,expires_at,used_by,revoked,created_at FROM invites ORDER BY created_at DESC LIMIT 500').all();return {invites:rows.results};}
    if(request.method==='POST'){
      const data=await input(request,10000),address=data.email?email(data.email):'',code=random(),id=crypto.randomUUID(),expires=new Date(Date.now()+7*86400000).toISOString();
      await env.DB.prepare('INSERT INTO invites(id,code_hash,email,expires_at,created_at) VALUES(?,?,?,?,?)').bind(id,await hash(code),address,expires,now()).run();return {code,expiresAt:expires,id};
    }
  }
  if(path==='/api/admin/invites/revoke'&&request.method==='POST'){const data=await input(request,10000);await env.DB.prepare('UPDATE invites SET revoked=1 WHERE id=? AND used_by IS NULL').bind(data.id).run();return {ok:true};}
  throw new ApiError('Método não permitido.',405);
}
