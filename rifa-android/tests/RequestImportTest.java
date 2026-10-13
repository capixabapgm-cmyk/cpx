import br.com.minhasrifas.RequestImport;import java.util.*;
public class RequestImportTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        Map<Integer,String> owners=new HashMap<>();owners.put(1,"");owners.put(2,"old");Map<String,List<Integer>> accepted=new LinkedHashMap<>();accepted.put("conflict",Arrays.asList(1));accepted.put("valid",Arrays.asList(3,4));accepted.put("old",Arrays.asList(2));
        Set<String> conflicts=RequestImport.conflicts(owners,accepted,new HashSet<>(Arrays.asList("old")));check(conflicts.equals(new HashSet<>(Arrays.asList("conflict"))));check(!conflicts.contains("valid"));
        accepted.put("duplicate",Arrays.asList(4));conflicts=RequestImport.conflicts(owners,accepted,new HashSet<>(Arrays.asList("old")));check(conflicts.containsAll(Arrays.asList("valid","duplicate","conflict")));check(!conflicts.contains("old"));
        owners.remove(1);accepted.remove("duplicate");check(RequestImport.conflicts(owners,accepted,new HashSet<>(Arrays.asList("old"))).isEmpty());
        System.out.println("Importação: conflito isolado não bloqueia pedido válido, não substitui venda manual e identifica aceitos sobrepostos.");
    }
}
