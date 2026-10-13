import br.com.minhasrifas.RequestNumbers;import java.util.*;
public class RequestNumbersTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        Map<String,String> owners=new LinkedHashMap<>();owners.put("1","cancelled");owners.put("2","");owners.put("3","kept");owners.put("4","cancelled");owners.put("5",null);
        Set<String> cancelled=new HashSet<>(Arrays.asList("cancelled",""));check(RequestNumbers.releasedKeys(owners,cancelled).equals(Arrays.asList("1","4")));
        check(RequestNumbers.releasedKeys(owners,Collections.emptySet()).isEmpty());
        check(RequestNumbers.detachedKeys(owners,cancelled,new HashSet<>(Arrays.asList("1"))).equals(Arrays.asList("4")));
        check(RequestNumbers.overlaps(Arrays.asList(1,2),Arrays.asList(2,3)));check(!RequestNumbers.overlaps(Arrays.asList(1,2),Arrays.asList(3,4)));
        System.out.println("Pedidos: libera somente números vinculados ao ID excluído, preserva vendas manuais e detecta números em comum.");
    }
}
