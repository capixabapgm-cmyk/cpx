import br.com.minhasrifas.OrderNoticeRules;
import java.util.*;
public class OrderNoticeRulesTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        Set<String> seen=new HashSet<>(Arrays.asList("old-pending","old-accepted"));
        Map<String,String> rows=new HashMap<>();rows.put("old-pending","accepted");rows.put("new-request","pending");
        // Count remains one, but a different request arrived: must notify.
        check(OrderNoticeRules.newPending(rows,seen)==1);
        seen.addAll(rows.keySet());check(OrderNoticeRules.newPending(rows,seen)==0);
        rows.put("rejected-new","rejected");rows.put("accepted-new","accepted");check(OrderNoticeRules.newPending(rows,seen)==0);
        rows.put("another-pending","pending");check(OrderNoticeRules.newPending(rows,seen)==1);
        check(OrderNoticeRules.newPending(Collections.emptyMap(),seen)==0);
        System.out.println("Notificações: pedido novo com mesma contagem, repetição e pedidos encerrados verificados.");
    }
}
