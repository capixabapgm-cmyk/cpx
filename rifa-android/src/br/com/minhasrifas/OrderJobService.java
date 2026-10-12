package br.com.minhasrifas;

import android.app.job.*;
import org.json.*;

public final class OrderJobService extends JobService {
    private volatile boolean stopped;
    private Thread worker;
    @Override public boolean onStartJob(JobParameters params){
        stopped=false;
        worker=new Thread(()->{
            try {
                android.content.SharedPreferences p=OrderNotifications.prefs(this);
                String session=p.getString("accountSession","");JSONObject profile=new JSONObject(p.getString("accountProfile","{}"));String user=profile.optString("id");
                if(session.isEmpty()||user.isEmpty()||!p.getBoolean("orderNotifications",true))return;
                JSONArray raffles=new JSONArray(p.getString("rifas:"+user,"[]"));
                for(int i=0;i<raffles.length()&&!stopped;i++){
                    if(!session.equals(p.getString("accountSession","")))break;
                    JSONObject raffle=raffles.getJSONObject(i);if(!raffle.has("webId"))continue;
                    try{
                        JSONObject data=OnlineClient.callWithSession(raffle.optString("webBase"),"/api/raffles/"+raffle.optString("webId")+"/requests","GET",raffle.optString("webToken"),-1,null,session,0);
                        if(!stopped&&session.equals(p.getString("accountSession","")))OrderNotifications.observe(this,user,raffle,data.getJSONArray("requests"));
                    }catch(Exception ignored){/* Network failures retry on the next scheduled check. No sales or backup writes. */}
                }
            }catch(Exception ignored){}finally{if(!stopped)jobFinished(params,false);}
        },"raffle-order-check");worker.start();return true;
    }
    @Override public boolean onStopJob(JobParameters params){stopped=true;if(worker!=null)worker.interrupt();return true;}
}
