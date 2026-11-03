package br.com.autopro.model;

public enum StatusAgendamento {
    AGENDADO("Agendado"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusAgendamento(String descricao) {
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
