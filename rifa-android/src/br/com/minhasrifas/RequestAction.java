package br.com.minhasrifas;

/** An order decision does not depend on publishing the raffle or saving a backup. */
public final class RequestAction {
    public interface Steps<T> {
        T refresh() throws Exception;
        String status(T latest) throws Exception;
        boolean conflict(T latest) throws Exception;
        T decide() throws Exception;
    }
    public static <T> T execute(Steps<T> steps,boolean accept) throws Exception {
        T latest=steps.refresh();String status=steps.status(latest);
        if("accepted".equals(status)){if(accept)return latest;throw new Exception("Este pedido já foi aceito. Use Excluir pedido para liberar seus números.");}
        if("rejected".equals(status)){if(!accept)return latest;throw new Exception("Este pedido já foi recusado ou excluído.");}
        if(!"pending".equals(status))throw new Exception("Pedido não encontrado. Atualize a lista de pedidos.");
        if(accept&&steps.conflict(latest))throw new Exception("Há números vendidos neste celular. Confira os duplicados ou escolha outros números.");
        return steps.decide();
    }
}
