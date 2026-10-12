const raffleId=/^[a-f0-9-]{36}$/;
const escape=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const imageHash=async value=>[...new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value)))].map(b=>b.toString(16).padStart(2,'0')).join('');
export async function linkPreview(request,env){
  const url=new URL(request.url);
  const imageMatch=url.pathname.match(/^\/api\/raffles\/([a-f0-9-]{36})\/image$/);
  if(imageMatch){
    if(!['GET','HEAD'].includes(request.method))return new Response(null,{status:405,headers:{Allow:'GET, HEAD'}});
    const row=await env.DB.prepare('SELECT image FROM raffles WHERE id=?').bind(imageMatch[1]).first();
    if(!row?.image||!/^data:image\/jpeg;base64,[A-Za-z0-9+/=]+$/.test(row.image)||row.image.length>250000)return new Response(null,{status:404});
    const etag='"'+await imageHash(row.image)+'"';const headers={'Content-Type':'image/jpeg','Cache-Control':'public, max-age=300','X-Content-Type-Options':'nosniff',ETag:etag};
    if(request.headers.get('If-None-Match')===etag)return new Response(null,{status:304,headers});
    const bytes=Uint8Array.from(atob(row.image.slice(row.image.indexOf(',')+1)),c=>c.charCodeAt(0));
    return new Response(request.method==='HEAD'?null:bytes,{headers});
  }
  if(!['/','/index.html'].includes(url.pathname)||!['GET','HEAD'].includes(request.method))return null;
  const id=url.searchParams.get('r');if(!raffleId.test(id??''))return null;
  const row=await env.DB.prepare('SELECT id,name,description,image FROM raffles WHERE id=?').bind(id).first();if(!row)return null;
  // Metadata is in the initial HTML: messaging crawlers do not need JavaScript.
  const asset=await env.ASSETS.fetch(new Request(new URL('/index.html',url.origin),{method:'GET'}));if(!asset.ok)return asset;
  const title=row.name.slice(0,200);const description=(row.description.trim()||'Escolha seus números e envie seu pedido ao organizador.').replace(/\s+/g,' ').slice(0,220);
  const canonical=url.origin+'/?r='+id;
  let tags=`<meta property="og:type" content="website"><meta property="og:site_name" content="Minhas Rifas"><meta property="og:locale" content="pt_BR"><meta property="og:title" content="${escape(title)}"><meta property="og:description" content="${escape(description)}"><meta property="og:url" content="${escape(canonical)}"><meta name="description" content="${escape(description)}"><meta name="twitter:title" content="${escape(title)}"><meta name="twitter:description" content="${escape(description)}">`;
  if(row.image){const image=url.origin+'/api/raffles/'+id+'/image?v='+(await imageHash(row.image)).slice(0,16);tags+=`<meta property="og:image" content="${escape(image)}"><meta property="og:image:secure_url" content="${escape(image)}"><meta property="og:image:type" content="image/jpeg"><meta property="og:image:alt" content="${escape(title)}"><meta name="twitter:card" content="summary_large_image"><meta name="twitter:image" content="${escape(image)}">`;}
  else tags+='<meta name="twitter:card" content="summary">';
  const html=(await asset.text()).replace(/<title>[\s\S]*?<\/title>/i,()=>`<title>${escape(title)} • Minhas Rifas</title>`).replace('</head>',()=>tags+'</head>');
  const headers=new Headers(asset.headers);headers.set('Content-Type','text/html; charset=utf-8');headers.set('Cache-Control','no-store');headers.delete('Content-Length');headers.delete('ETag');headers.delete('Content-Encoding');
  return new Response(request.method==='HEAD'?null:html,{status:asset.status,headers});
}
