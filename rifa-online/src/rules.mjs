export class ApiError extends Error { constructor(message,status=400){super(message);this.status=status;} }
export function text(value,label,max,required=true){
  if(typeof value!=='string'||value.length>max||(required&&!value.trim()))throw new ApiError(`${label} inválido.`);
  return value.trim();
}
export function numbers(value,total,max=10000){
  if(!Array.isArray(value)||value.length>max||value.some(n=>!Number.isInteger(n)||n<1||n>total))throw new ApiError('Números inválidos.');
  return [...new Set(value)].sort((a,b)=>a-b);
}
export function raffleData(body){
  if(!Number.isInteger(body.total)||body.total<1||body.total>10000)throw new ApiError('Quantidade inválida.');
  const image=body.image??'';
  if(typeof image!=='string'||image.length>250000||image&&!/^data:image\/jpeg;base64,[A-Za-z0-9+/=]+$/.test(image))throw new ApiError('Imagem inválida.');
  if(typeof body.closed!=='boolean')throw new ApiError('Estado da rifa inválido.');
  return {name:text(body.name,'Nome',200),description:text(body.description,'Descrição',4000,false),total:body.total,sold:numbers(body.sold,body.total),closed:body.closed,image};
}
export function requestData(body,raffle){
  if(raffle.closed)throw new ApiError('Esta rifa já foi encerrada.',409);
  const selected=numbers(body.numbers,raffle.total,100);
  if(!selected.length)throw new ApiError('Selecione pelo menos um número.');
  const occupied=new Set(JSON.parse(raffle.sold));
  if(selected.some(n=>occupied.has(n)))throw new ApiError('Algum número já foi vendido. Atualize a página e escolha novamente.',409);
  const phone=text(body.phone,'Telefone',40);
  if(phone.replace(/\D/g,'').length<8)throw new ApiError('Informe um telefone com DDD.');
  return {name:text(body.name,'Nome',120),phone,numbers:selected};
}
export function publicRaffle(row){
  return {id:row.id,name:row.name,description:row.description,total:row.total,sold:JSON.parse(row.sold),closed:!!row.closed,image:row.image,revision:row.revision,pixName:row.pix_name??'Francisco Claudemir da Silva Andrade',pixKey:row.pix_key??'78216834268'};
}
