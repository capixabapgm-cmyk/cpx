import {ApiError} from './rules.mjs';
export const THEMES=['green','red','pink','yellow','blue','purple'];
export const themeSchema="CREATE TABLE IF NOT EXISTS account_themes (user_id TEXT PRIMARY KEY REFERENCES users(id),theme TEXT NOT NULL DEFAULT 'green')";
export async function accountTheme(env,userId){
  if(!userId)return 'green';
  try {const row=await env.DB.prepare('SELECT theme FROM account_themes WHERE user_id=?').bind(userId).first();return THEMES.includes(row?.theme)?row.theme:'green';}
  catch(e){if(/no such table.*account_themes/i.test(String(e.message)))return 'green';throw e;}
}
export async function saveTheme(env,userId,theme){
  if(!THEMES.includes(theme))throw new ApiError('Escolha uma das seis cores disponíveis.');
  await env.DB.prepare(themeSchema).run();
  await env.DB.prepare('INSERT INTO account_themes(user_id,theme) VALUES(?,?) ON CONFLICT(user_id) DO UPDATE SET theme=excluded.theme').bind(userId,theme).run();
}
