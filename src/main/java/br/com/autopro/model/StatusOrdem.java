package br.com.autopro.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de uma ordem de serviço e as transições permitidas entre eles.
 * <pre>
 * AGUARDANDO_PECAS -> EM_SERVICO -> PRONTO_PARA_ENTREGA -> ENTREGUE
 * </pre>
 */
public enum StatusOrdem {
    AGUARDANDO_PECAS("Aguardando peças"),
    EM_SERVICO("Em serviço"),
    PRONTO_PARA_ENTREGA("Pronto para entrega"),
    ENTREGUE("Entregue");

    private final String descricao;

    StatusOrdem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Estados para os quais é permitido avançar a partir deste. */
    public Set<StatusOrdem> proximosPermitidos() {
        switch (this) {
            case AGUARDANDO_PECAS:
                return EnumSet.of(EM_SERVICO);
            case EM_SERVICO:
                return EnumSet.of(PRONTO_PARA_ENTREGA);
            case PRONTO_PARA_ENTREGA:
                return EnumSet.of(ENTREGUE);
            default:
                return EnumSet.noneOf(StatusOrdem.class);
        }
    }

    public boolean podeTransicionarPara(StatusOrdem destino) {
        return destino != null && proximosPermitidos().contains(destino);
    }

    /** Itens (peças) só podem ser alterados antes da baixa de estoque. */
    public boolean permiteAlterarItens() {
        return this == AGUARDANDO_PECAS;
    }

    /** Descrição e horas de mão de obra podem ser ajustadas até o serviço ser concluído. */
    public boolean permiteAlterarServico() {
        return this == AGUARDANDO_PECAS || this == EM_SERVICO;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
