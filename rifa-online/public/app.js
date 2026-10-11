const el=id=>document.getElementById(id);
const id=new URL(location.href).searchParams.get('r');
let raffle,selected=new Set(),sending=false,pendingId=null;
const fmt=n=>String(n).padStart(3,'0');
function summary(){el('selection').textContent=selected.size?`${selected.size} número(s): ${[...selected].sort((a,b)=>a-b).map(fmt).join(', ')}`:'Selecione os números acima.';}
function render(){
  el('name').textContent=raffle.name;el('description').textContent=raffle.description;
  el('image').hidden=!raffle.image;if(raffle.image)el('image').src=raffle.image;
  el('form').hidden=raffle.closed;
  el('pix').hidden=raffle.closed;
  if(raffle.closed){el('message').textContent='Esta rifa já foi encerrada.';return;}
  const sold=new Set(raffle.sold);selected=new Set([...selected].filter(n=>!sold.has(n)));
  const fragment=document.createDocumentFragment();
  for(let n=1;n<=raffle.total;n++){
    const button=document.createElement('button');button.type='button';button.textContent=fmt(n);button.disabled=sold.has(n);button.setAttribute('aria-pressed',selected.has(n)?'true':'false');button.setAttribute('aria-label',`Número ${n}${sold.has(n)?', vendido':''}`);
    button.onclick=()=>{if(sending)return;if(selected.has(n))selected.delete(n);else{if(selected.size>=100){el('message').textContent='Escolha até 100 números por pedido.';return;}selected.add(n);}pendingId=null;button.setAttribute('aria-pressed',selected.has(n)?'true':'false');summary();};fragment.append(button);
  }
  el('grid').replaceChildren(fragment);summary();
}
async function load(){
  if(!/^[a-f0-9-]{36}$/.test(id??'')){el('description').textContent='Abra o link da rifa enviado pelo organizador.';return;}
  el('refresh').disabled=true;
  try{const response=await fetch(`/api/raffles/${id}`,{cache:'no-store'});const data=await response.json();if(!response.ok)throw new Error(data.error);raffle=data;render();el('message').textContent='';}catch(e){el('message').textContent=e.message||'Não foi possível carregar. Tente novamente.';}finally{el('refresh').disabled=false;}
}
el('refresh').onclick=load;
el('copy-pix').onclick=async()=>{
  const key=el('pix-key');
  try {await navigator.clipboard.writeText(key.value);el('pix-feedback').textContent='Chave Pix copiada.';}
  catch {key.focus();key.select();key.setSelectionRange(0,key.value.length);let copied=false;try{copied=document.execCommand('copy');}catch{}el('pix-feedback').textContent=copied?'Chave Pix copiada.':'A chave está selecionada. Toque e segure para copiar.';}
};
el('buyer').oninput=el('phone').oninput=()=>{pendingId=null;};
el('order').onsubmit=async event=>{
  event.preventDefault();if(sending)return;
  if(!selected.size){el('message').textContent='Selecione ao menos um número.';return;}
  const name=el('buyer').value.trim(),phone=el('phone').value.trim();
  if(!name||phone.replace(/\D/g,'').length<8){el('message').textContent='Informe seu nome e telefone com DDD.';return;}
  pendingId??=crypto.randomUUID();const numbers=[...selected].sort((a,b)=>a-b);
  sending=true;el('send').disabled=true;el('send').textContent='Enviando…';el('message').textContent='';
  try{
    const response=await fetch(`/api/raffles/${id}/requests`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({id:pendingId,name,phone,numbers})});
    const data=await response.json();if(!response.ok)throw new Error(data.error);
    el('form').hidden=true;el('success').hidden=false;el('summary').textContent=`${name} — ${numbers.map(fmt).join(', ')}`;el('reference').textContent=`Referência: ${data.id}`;
  }catch(e){el('message').textContent=e.message||'Falha de conexão. Tente enviar novamente.';}finally{sending=false;el('send').disabled=false;el('send').textContent='Enviar pedido';}
};
load();
