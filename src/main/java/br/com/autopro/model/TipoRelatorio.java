package br.com.autopro.model;

/** Relatórios disponíveis e os filtros que cada um aceita. */
public enum TipoRelatorio {
    ORDENS_POR_PERIODO("Ordens de serviço por período", true, true, true, true),
    ORDENS_POR_CLIENTE("Ordens por cliente (resumo)", true, false, true, true),
    HISTORICO_VEICULO("Histórico por veículo", true, true, true, false),
    PECAS_POR_VEICULO("Peças utilizadas por veículo", true, true, true, false),
    SERVICOS_CONCLUIDOS("Serviços concluídos", true, true, true, false),
    PAGAMENTOS("Pagamentos registrados", true, true, true, false),
    CLIENTES_VIP("Clientes VIP", true, false, false, false),
    REVISAO_VENCIDA("Veículos com revisão vencida", true, true, false, false),
    PECAS_ABAIXO_MINIMO("Peças abaixo do estoque mínimo", false, false, false, false),
    AGENDAMENTOS_FUTUROS("Agendamentos futuros", true, true, true, false);

    private final String descricao;
    private final boolean filtraCliente;
    private final boolean filtraPlaca;
    private final boolean filtraPeriodo;
    private final boolean filtraStatus;

    TipoRelatorio(String descricao, boolean filtraCliente, boolean filtraPlaca,
                  boolean filtraPeriodo, boolean filtraStatus) {
        this.descricao = descricao;
        this.filtraCliente = filtraCliente;
        this.filtraPlaca = filtraPlaca;
        this.filtraPeriodo = filtraPeriodo;
        this.filtraStatus = filtraStatus;
    }

    public String getDescricao() { return descricao; }
    public boolean filtraCliente() { return filtraCliente; }
    public boolean filtraPlaca() { return filtraPlaca; }
    public boolean filtraPeriodo() { return filtraPeriodo; }
    public boolean filtraStatus() { return filtraStatus; }

    @Override
    public String toString() {
        return descricao;
    }
}
