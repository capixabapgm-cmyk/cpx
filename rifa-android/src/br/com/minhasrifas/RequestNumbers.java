package br.com.minhasrifas;
import java.util.*;
public final class RequestNumbers {
    public static ArrayList<String> releasedKeys(Map<String,String> owners,Set<String> cancelled){
        ArrayList<String> keys=new ArrayList<>();for(Map.Entry<String,String> sale:owners.entrySet())if(sale.getValue()!=null&&!sale.getValue().isEmpty()&&cancelled.contains(sale.getValue()))keys.add(sale.getKey());return keys;
    }
    public static boolean overlaps(Collection<Integer> first,Collection<Integer> second){Set<Integer> values=new HashSet<>(first);for(int n:second)if(values.contains(n))return true;return false;}
}
