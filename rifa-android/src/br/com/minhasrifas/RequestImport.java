package br.com.minhasrifas;
import java.util.*;
public final class RequestImport {
    public static Set<String> conflicts(Map<Integer,String> owners,Map<String,List<Integer>> accepted,Set<String> imported){
        Set<String> conflicts=new HashSet<>();Map<Integer,Set<String>> claims=new HashMap<>();
        for(Map.Entry<String,List<Integer>> order:accepted.entrySet())for(int n:order.getValue()){
            Set<String> ids=claims.get(n);if(ids==null){ids=new HashSet<>();claims.put(n,ids);}ids.add(order.getKey());
            if(!imported.contains(order.getKey())&&owners.containsKey(n)&&!order.getKey().equals(owners.get(n)))conflicts.add(order.getKey());
        }
        for(Set<String> ids:claims.values())if(ids.size()>1)conflicts.addAll(ids);
        return conflicts;
    }
}
