package br.com.minhasrifas;
import java.util.*;
public final class OrderNoticeRules {
    public static int newPending(Map<String,String> statuses,Set<String> seen){
        int count=0;for(Map.Entry<String,String> order:statuses.entrySet())if("pending".equals(order.getValue())&&!seen.contains(order.getKey()))count++;return count;
    }
}
