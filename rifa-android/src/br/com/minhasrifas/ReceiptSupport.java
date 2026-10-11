package br.com.minhasrifas;

import java.util.*;

public final class ReceiptSupport {
    public static String buyerKey(String name, String phone) {
        String normalizedName=name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return normalizedName+"\n"+phone.replaceAll("\\D", "");
    }
    public static String numbers(Collection<Integer> numbers) {
        StringBuilder s=new StringBuilder();
        for(int n:new TreeSet<Integer>(numbers)) { if(s.length()>0)s.append(", "); s.append(String.format(Locale.US,"%03d",n)); }
        return s.toString();
    }
    public static String receipt(String raffle,String description,String name,String phone,Collection<Integer> selected,String issuedAt) {
        return "COMPROVANTE DE PARTICIPAÇÃO\n\nRifa: "+raffle+
            (description.isEmpty()?"":"\nDescrição: "+description)+"\nComprador: "+name+
            (phone.isEmpty()?"":"\nTelefone: "+phone)+"\nQuantidade: "+new TreeSet<Integer>(selected).size()+
            "\nNúmeros: "+numbers(selected)+"\n\nEmitido em: "+issuedAt+
            "\nRegistro dos números cadastrados nesta rifa. Não comprova pagamento.";
    }
}
