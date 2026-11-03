package br.com.autopro.model;

public enum TipoAgendamento {
    REVISAO_PREVENTIVA("Revisão preventiva"),
    SERVICO("Serviço"),
    REVISAO_RAPIDA("Revisão rápida");

    private final String descricao;

    TipoAgendamento(String descricao) {
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
