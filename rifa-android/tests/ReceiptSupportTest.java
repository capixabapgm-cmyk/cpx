import br.com.minhasrifas.ReceiptSupport;
import java.util.*;

public class ReceiptSupportTest {
    static void check(boolean condition) { if(!condition)throw new AssertionError(); }
    public static void main(String[] args) {
        check(ReceiptSupport.buyerKey(" Ana  Silva ","(11) 99999-0000").equals(ReceiptSupport.buyerKey("ana silva","11999990000")));
        check(!ReceiptSupport.buyerKey("Ana","11999990000").equals(ReceiptSupport.buyerKey("Ana","11999990001")));
        check(ReceiptSupport.buyerKey("Ana","").equals(ReceiptSupport.buyerKey(" ANA ","")));
        check(ReceiptSupport.numbers(Arrays.asList(12,2,1,2)).equals("001, 002, 012"));
        String text=ReceiptSupport.receipt("Bicicleta","Prêmio azul","Ana","",Arrays.asList(12,2,1,2),"10/10/2026 18:00");
        check(text.contains("Rifa: Bicicleta") && text.contains("Comprador: Ana"));
        check(text.contains("Quantidade: 3") && text.contains("Números: 001, 002, 012"));
        check(!text.contains("Telefone:"));
        check(text.contains("Não comprova pagamento."));
        String personal=ReceiptSupport.receipt("Rifa","","Ana","",Arrays.asList(1),"hoje","Outro organizador","chave-outro");
        check(personal.contains("chave-outro")&&!personal.contains("78216834268"));
        check(!ReceiptSupport.receipt("Rifa","","Ana","",Arrays.asList(1),"hoje","","").contains("78216834268"));
        check(ReceiptSupport.receipt("Rifa","","Ana","11999990000",Arrays.asList(1),"hoje").contains("Telefone: 11999990000"));
        System.out.println("Testes de agrupamento e comprovante passaram.");
    }
}
