package br.com.autopro.model;

public enum SituacaoPagamento {
    PENDENTE("Pendente"),
    PAGO("Pago");

    private final String descricao;

    SituacaoPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
