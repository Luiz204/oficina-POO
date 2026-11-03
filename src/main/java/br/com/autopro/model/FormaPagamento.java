package br.com.autopro.model;

public enum FormaPagamento {
    PIX("PIX"),
    DINHEIRO("Dinheiro"),
    CARTAO("Cartão"),
    TRANSFERENCIA("Transferência");

    private final String descricao;

    FormaPagamento(String descricao) {
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
