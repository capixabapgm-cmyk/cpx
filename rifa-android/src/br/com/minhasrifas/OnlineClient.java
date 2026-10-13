package br.com.minhasrifas;
import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;

public final class OnlineClient {
    public static final String DEFAULT_BASE="https://minhas-rifas-pedidos.franciscoandrade-photo.workers.dev";
    public static volatile String session="";
    public static volatile int backupRevision=0;
    public static String base(String input) throws Exception {
        URI uri=new URI(input.trim());
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||uri.getPort()!=-1||(!uri.getPath().isEmpty()&&!"/".equals(uri.getPath())))throw new Exception("Use apenas o endereço HTTPS da hospedagem, sem caminho ou parâmetros.");
        return "https://"+uri.getHost();
    }
    public static JSONObject call(String base,String path,String method,String token,int revision,JSONObject body) throws Exception {
        return callWithSession(base,path,method,token,revision,body,session,backupRevision);
    }
    public static JSONObject callWithSession(String base,String path,String method,String token,int revision,JSONObject body,String accountSession,int accountRevision) throws Exception {
        HttpsURLConnection c=(HttpsURLConnection)new URL(base(base)+path).openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestMethod(method);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("X-Request-Changes","1");
        if(!accountSession.isEmpty()&&base(base).equals(DEFAULT_BASE)){c.setRequestProperty("X-Session-Token",accountSession);c.setRequestProperty("X-Backup-Revision",""+accountRevision);}
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        if(revision>=0)c.setRequestProperty("If-Match",""+revision);
        try {
            if(body!=null){byte[] bytes=body.toString().getBytes("UTF-8");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
            int code=c.getResponseCode();InputStream input=code>=400?c.getErrorStream():c.getInputStream();
            if(input==null)throw new Exception("Servidor indisponível ("+code+").");
            ByteArrayOutputStream output=new ByteArrayOutputStream();try(InputStream in=input){byte[] buffer=new byte[4096];int read;while((read=in.read(buffer))!=-1){output.write(buffer,0,read);if(output.size()>3000000)throw new Exception("Resposta muito grande.");}}
            JSONObject response;try{response=new JSONObject(new String(output.toByteArray(),"UTF-8"));}catch(Exception e){throw new Exception("O endereço não corresponde ao serviço Minhas Rifas.");}
            if(code<200||code>=300)throw new Exception(response.optString("error","Erro de conexão ("+code+")."));return response;
        }finally{c.disconnect();}
    }
}
