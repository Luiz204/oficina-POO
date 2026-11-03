package br.com.autopro.ui;

import br.com.autopro.model.Agendamento;
import br.com.autopro.model.HistoricoStatus;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.MovimentacaoEstoque;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.model.Revisao;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.model.TipoMovimentacao;
import br.com.autopro.ui.componentes.ModeloTabela;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Definições de tabelas reutilizadas em vários módulos (evita duplicação de colunas). */
final class FabricaTabelas {

    private FabricaTabelas() {
    }

    static String nomeComVip(String nome, boolean vip) {
        return vip ? nome + "  " + Tema.MARCA_VIP : nome;
    }

    static Tabela<OrdemServico> ordens() {
        ModeloTabela<OrdemServico> m = new ModeloTabela<OrdemServico>()
                .coluna("OS", Integer.class, OrdemServico::getId)
                .coluna("Abertura", LocalDateTime.class, OrdemServico::getDataAbertura)
                .coluna("Cliente", o -> nomeComVip(o.getClienteNome(), o.isClienteVip()))
                .coluna("Placa", OrdemServico::getPlaca)
                .coluna("Modelo", OrdemServico::getModeloVeiculo)
                .coluna("Descrição", OrdemServico::getDescricao)
                .coluna("Status", StatusOrdem.class, OrdemServico::getStatus)
                .coluna("Total", BigDecimal.class, OrdemServico::getValorTotal)
                .coluna("Pagamento", OrdemServico::getSituacaoPagamento);
        return new Tabela<>(m).destacar(OrdemServico::isClienteVip, Tema.DESTAQUE_VIP)
                .larguras(50, 120, 190, 80, 140, 240, 140, 100, 85);
    }

    static Tabela<ItemOrdem> itens() {
        ModeloTabela<ItemOrdem> m = new ModeloTabela<ItemOrdem>()
                .coluna("OS", Integer.class, ItemOrdem::getOrdemId)
                .coluna("Código", ItemOrdem::getPecaCodigo)
                .coluna("Peça", ItemOrdem::getPecaNome)
                .coluna("Qtd.", Integer.class, ItemOrdem::getQuantidade)
                .coluna("Preço unit. (no serviço)", BigDecimal.class, ItemOrdem::getPrecoUnitario)
                .coluna("Subtotal", BigDecimal.class, ItemOrdem::getSubtotal);
        return new Tabela<>(m).larguras(50, 110, 240, 50, 150, 110);
    }

    static Tabela<Revisao> revisoes() {
        ModeloTabela<Revisao> m = new ModeloTabela<Revisao>()
                .coluna("Data", LocalDate.class, Revisao::getDataRevisao)
                .coluna("Quilometragem", Integer.class, Revisao::getKm)
                .coluna("Observações", Revisao::getObservacoes);
        return new Tabela<>(m).larguras(100, 110, 520);
    }

    static Tabela<MovimentacaoEstoque> movimentacoes() {
        ModeloTabela<MovimentacaoEstoque> m = new ModeloTabela<MovimentacaoEstoque>()
                .coluna("Data/hora", LocalDateTime.class, MovimentacaoEstoque::getDataHora)
                .coluna("Peça", x -> x.getPecaCodigo() + " - " + x.getPecaNome())
                .coluna("Tipo", MovimentacaoEstoque::getTipo)
                .coluna("Qtd.", Integer.class, MovimentacaoEstoque::getQuantidade)
                .coluna("Preço unit.", BigDecimal.class, MovimentacaoEstoque::getPrecoUnitario)
                .coluna("OS", Integer.class, MovimentacaoEstoque::getOrdemId)
                .coluna("Placa", MovimentacaoEstoque::getPlaca)
                .coluna("Observação", MovimentacaoEstoque::getObservacao);
        return new Tabela<>(m)
                .corTexto(x -> x.getTipo() == TipoMovimentacao.SAIDA ? Tema.PERIGO : Tema.SUCESSO)
                .larguras(120, 220, 70, 50, 90, 50, 80, 200);
    }

    static Tabela<HistoricoStatus> historicoStatus() {
        ModeloTabela<HistoricoStatus> m = new ModeloTabela<HistoricoStatus>()
                .coluna("Data/hora", LocalDateTime.class, HistoricoStatus::getDataHora)
                .coluna("Status anterior", h -> h.getStatusAnterior() == null ? "-" : h.getStatusAnterior())
                .coluna("Novo status", StatusOrdem.class, HistoricoStatus::getStatusNovo)
                .coluna("Observação", HistoricoStatus::getObservacao);
        return new Tabela<>(m).larguras(130, 150, 150, 380);
    }

    static Tabela<Agendamento> agendamentos() {
        ModeloTabela<Agendamento> m = new ModeloTabela<Agendamento>()
                .coluna("Data/hora", LocalDateTime.class, Agendamento::getDataHora)
                .coluna("Tipo", Agendamento::getTipo)
                .coluna("Placa", Agendamento::getPlaca)
                .coluna("Modelo", Agendamento::getModeloVeiculo)
                .coluna("Cliente", a -> nomeComVip(a.getClienteNome(), a.isClienteVip()))
                .coluna("Telefone", Agendamento::getClienteTelefone)
                .coluna("Duração (min)", Integer.class, Agendamento::getDuracaoMinutos)
                .coluna("Descrição", Agendamento::getDescricao)
                .coluna("Status", Agendamento::getStatus)
                .coluna("OS", Integer.class, Agendamento::getOrdemId);
        return new Tabela<>(m).destacar(Agendamento::isClienteVip, Tema.DESTAQUE_VIP)
                .larguras(120, 130, 80, 140, 180, 120, 90, 220, 90, 50);
    }

    static Tabela<Pagamento> pagamentos() {
        ModeloTabela<Pagamento> m = new ModeloTabela<Pagamento>()
                .coluna("Nº", Integer.class, Pagamento::getId)
                .coluna("Data/hora", LocalDateTime.class, Pagamento::getDataHora)
                .coluna("OS", Integer.class, Pagamento::getOrdemId)
                .coluna("Cliente", Pagamento::getClienteNome)
                .coluna("Placa", Pagamento::getPlaca)
                .coluna("Forma", Pagamento::getForma)
                .coluna("Valor", BigDecimal.class, Pagamento::getValor)
                .coluna("Situação", p -> "Confirmado")
                .coluna("Observação", Pagamento::getObservacao);
        return new Tabela<>(m).larguras(50, 130, 50, 200, 80, 110, 100, 90, 200);
    }
}
