package br.com.autopro.model;

/** Resultado da avaliação da necessidade de revisão de um veículo. */
public enum SituacaoRevisao {
    EM_DIA("Em dia", false),
    VENCIDA_POR_KM("Vencida (km)", true),
    VENCIDA_POR_DATA("Vencida (data)", true),
    VENCIDA_POR_KM_E_DATA("Vencida (km e data)", true),
    SEM_HISTORICO("Sem histórico - avaliar", true);

    private final String descricao;
    private final boolean requerAtencao;

    SituacaoRevisao(String descricao, boolean requerAtencao) {
        this.descricao = descricao;
        this.requerAtencao = requerAtencao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Verdadeiro quando o veículo deve aparecer na lista de revisões vencidas/pendentes. */
    public boolean requerAtencao() {
        return requerAtencao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
