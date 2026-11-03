package br.com.autopro.model;

/** Categoria do cliente. Clientes VIP têm desconto na mão de obra e prioridade no atendimento. */
public enum CategoriaCliente {
    COMUM("Comum"),
    VIP("VIP");

    private final String descricao;

    CategoriaCliente(String descricao) {
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
