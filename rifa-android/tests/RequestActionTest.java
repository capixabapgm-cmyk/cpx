import br.com.minhasrifas.RequestAction;import java.util.*;
public class RequestActionTest {
    static class Fake implements RequestAction.Steps<String>{String state="pending";boolean occupied=false;List<String> calls=new ArrayList<>();public String refresh(){calls.add("GET pedidos");return state;}public String status(String latest){return latest;}public boolean conflict(String latest){return occupied;}public String decide(){calls.add("POST decisão");return "accepted";}}
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        Fake pending=new Fake();check(RequestAction.execute(pending,true).equals("accepted"));check(pending.calls.equals(Arrays.asList("GET pedidos","POST decisão")));
        Fake accepted=new Fake();accepted.state="accepted";check(RequestAction.execute(accepted,true).equals("accepted"));check(accepted.calls.size()==1);
        Fake conflict=new Fake();conflict.occupied=true;try{RequestAction.execute(conflict,true);throw new AssertionError();}catch(Exception expected){check(conflict.calls.size()==1);}
        Fake rejected=new Fake();rejected.state="rejected";try{RequestAction.execute(rejected,true);throw new AssertionError();}catch(Exception expected){check(rejected.calls.size()==1);}
        Fake refusal=new Fake();refusal.occupied=true;RequestAction.execute(refusal,false);check(refusal.calls.size()==2);
        System.out.println("Aceitação: consulta e decisão sem publicação/backup, repetição idempotente e proteção de números ocupados verificadas.");
    }
}
