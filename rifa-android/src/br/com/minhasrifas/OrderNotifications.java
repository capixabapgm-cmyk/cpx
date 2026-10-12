package br.com.minhasrifas;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.*;

final class OrderNotifications {
    static final int JOB=1801;
    static final String CHANNEL="new_orders";
    static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("MainActivity",0);}
    static void schedule(Context c){
        if(!prefs(c).getBoolean("orderNotifications",true)||prefs(c).getString("accountSession","").isEmpty())return;
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Novos pedidos de rifa",NotificationManager.IMPORTANCE_DEFAULT));
        JobScheduler js=c.getSystemService(JobScheduler.class);
        if(js.getPendingJob(JOB)==null)js.schedule(new JobInfo.Builder(JOB,new ComponentName(c,OrderJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());
    }
    static void clear(Context c){c.getSystemService(JobScheduler.class).cancel(JOB);c.getSystemService(NotificationManager.class).cancelAll();}
    // Share the same persistent ledger between foreground polling and background checks.
    static synchronized void observe(Context c,String user,JSONObject raffle,JSONArray rows) throws Exception {
        android.content.SharedPreferences p=prefs(c);
        if(!p.getBoolean("orderNotifications",true))return;
        JSONObject active=new JSONObject(p.getString("accountProfile","{}"));
        if(!user.equals(active.optString("id")))return;
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        if(!nm.areNotificationsEnabled())return;
        String key="ordersSeen:"+user+":"+raffle.optString("webId");
        Set<String> seen=new HashSet<>(p.getStringSet(key,Collections.emptySet()));
        // Already cached orders are historical, including on upgrade to 1.8.
        if(!p.contains(key)){JSONArray cached=raffle.optJSONArray("webRequests");if(cached!=null)for(int i=0;i<cached.length();i++)seen.add(cached.getJSONObject(i).optString("id"));}
        int pending=0;Map<String,String> statuses=new HashMap<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String id=row.optString("id");statuses.put(id,row.optString("status"));if("pending".equals(row.optString("status")))pending++;}
        int fresh=OrderNoticeRules.newPending(statuses,seen);seen.addAll(statuses.keySet());
        String tag=user+":"+raffle.optString("webId");
        if(pending==0)nm.cancel(tag,1);
        if(fresh>0){
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Novos pedidos de rifa",NotificationManager.IMPORTANCE_DEFAULT));
            Intent intent=new Intent(c,MainActivity.class).setAction("orders:"+tag).putExtra("orderRaffle",raffle.optString("webId"));
            PendingIntent open=PendingIntent.getActivity(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification notice=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.notification).setContentTitle("Novo pedido • "+raffle.optString("name"))
                .setContentText(fresh+" novo(s) pedido(s). "+pending+" pendente(s) para conferir.").setContentIntent(open).setAutoCancel(true).setNumber(pending).setCategory(Notification.CATEGORY_EVENT).setVisibility(Notification.VISIBILITY_PRIVATE).build();
            nm.notify(tag,1,notice);
        }
        p.edit().putStringSet(key,seen).commit();
    }
}
