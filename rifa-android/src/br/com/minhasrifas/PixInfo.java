package br.com.minhasrifas;
public final class PixInfo {
    public static final String NAME="Francisco Claudemir da Silva Andrade";
    public static final String KEY="78216834268";
    public static String instructions() {
        return instructions(NAME,KEY);
    }
    public static String instructions(String name,String key) {
        if(key.isEmpty())return "O organizador ainda não informou sua chave Pix.";
        return "Pix\nDestinatário: "+name+"\nChave: "+key+"\nConfira o nome do destinatário no aplicativo do banco antes de confirmar. Combine o valor com o organizador. Pagamento conferido manualmente.";
    }
}
