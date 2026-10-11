import br.com.minhasrifas.PaymentRules;
import java.util.*;
public class PaymentRulesTest {
    static void check(boolean result){if(!result)throw new AssertionError();}
    public static void main(String[] args) {
        check(PaymentRules.paid(null));check(PaymentRules.paid(true));check(!PaymentRules.paid(false));
        Map<Integer,Boolean> sales=new HashMap<>();sales.put(1,false);sales.put(2,true);sales.put(8,null);sales.put(10,false);sales.put(100,true);
        ArrayList<Integer> eligible=PaymentRules.eligible(sales);
        check(eligible.equals(Arrays.asList(2,8,100)));
        for(int i=0;i<eligible.size();i++)check(PaymentRules.pick(eligible,i)==eligible.get(i));
        check(!eligible.contains(1)&&!eligible.contains(10)&&!eligible.contains(3));
        sales.put(2,false);sales.put(8,false);sales.put(100,false);check(PaymentRules.eligible(sales).isEmpty());
        boolean rejected=false;try{PaymentRules.pick(PaymentRules.eligible(sales),0);}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        sales.put(10,true);check(PaymentRules.pick(PaymentRules.eligible(sales),0)==10);
        System.out.println("Pagamentos: padrão legado pago, exclusão de não pagos, sem elegíveis e único pago verificados.");
    }
}
