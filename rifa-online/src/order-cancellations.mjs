export const cancellationSchema='CREATE TABLE IF NOT EXISTS request_cancellations (request_id TEXT PRIMARY KEY REFERENCES requests(id),raffle_id TEXT NOT NULL REFERENCES raffles(id),cancelled_at TEXT NOT NULL)';
export async function cancelledIds(env,raffleId){
  try{return (await env.DB.prepare('SELECT request_id FROM request_cancellations WHERE raffle_id=?').bind(raffleId).all()).results.map(row=>row.request_id);}
  catch(e){if(/no such table.*request_cancellations/i.test(String(e.message)))return [];throw e;}
}
