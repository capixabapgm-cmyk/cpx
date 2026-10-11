package br.com.minhasrifas;
import java.util.*;
public final class PaymentRules {
    public static boolean paid(Boolean value) { return value==null || value; }
    public static ArrayList<Integer> eligible(Map<Integer,Boolean> sales) {
        ArrayList<Integer> result=new ArrayList<>();
        for(int n:new TreeSet<Integer>(sales.keySet()))if(paid(sales.get(n)))result.add(n);
        return result;
    }
    public static int pick(List<Integer> eligible,int index) {
        if(eligible.isEmpty())throw new IllegalArgumentException("Nenhum número pago");
        return eligible.get(index);
    }
}
