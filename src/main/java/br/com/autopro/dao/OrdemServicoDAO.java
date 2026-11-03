package br.com.autopro.dao;

import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.SituacaoPagamento;
import br.com.autopro.model.StatusOrdem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrdemServicoDAO {

    private static final String SELECT = "SELECT o.*, v.placa, v.modelo, c.nome AS cliente_nome, "
            + "c.categoria AS cliente_categoria FROM ordens_servico o "
            + "JOIN veiculos v ON v.id = o.veiculo_id JOIN clientes c ON c.id = o.cliente_id ";

    /** Ordenação de atendimento: ordens abertas antes, clientes VIP primeiro, depois a mais antiga. */
    private static final String ORDEM_PRIORIDADE = " ORDER BY (o.status = 'ENTREGUE'), "
            + "(c.categoria = 'VIP') DESC, o.data_abertura";

    public int inserir(Connection c, OrdemServico o) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO ordens_servico (veiculo_id, cliente_id, descricao, horas_mao_obra, "
                        + "data_abertura, data_agendada, status, valor_pecas, valor_mao_obra, desconto, valor_total, "
                        + "situacao_pagamento) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                o.getVeiculoId(), o.getClienteId(), o.getDescricao(), o.getHorasMaoObra(),
                java.time.LocalDateTime.now().withNano(0), o.getDataAgendada(),
                o.getStatus(), o.getValorPecas(), o.getValorMaoObra(), o.getDesconto(), o.getValorTotal(),
                o.getSituacaoPagamento());
    }

    public void atualizar(Connection c, OrdemServico o) throws SQLException {
        JdbcUtil.executar(c, "UPDATE ordens_servico SET descricao = ?, horas_mao_obra = ?, data_agendada = ?, "
                        + "status = ?, valor_pecas = ?, valor_mao_obra = ?, desconto = ?, valor_total = ?, "
                        + "situacao_pagamento = ?, estoque_baixado = ?, data_conclusao = ?, data_entrega = ? "
                        + "WHERE id = ?",
                o.getDescricao(), o.getHorasMaoObra(), o.getDataAgendada(), o.getStatus(), o.getValorPecas(),
                o.getValorMaoObra(), o.getDesconto(), o.getValorTotal(), o.getSituacaoPagamento(),
                o.isEstoqueBaixado(), o.getDataConclusao(), o.getDataEntrega(), o.getId());
    }

    public boolean excluir(Connection c, int id) throws SQLException {
        return JdbcUtil.executar(c, "DELETE FROM ordens_servico WHERE id = ?", id) > 0;
    }

    /** Bloqueia a linha da ordem até o fim da transação (SELECT ... FOR UPDATE). */
    public boolean bloquear(Connection c, int id) throws SQLException {
        return JdbcUtil.existe(c, "SELECT id FROM ordens_servico WHERE id = ? FOR UPDATE", id);
    }

    public Optional<OrdemServico> buscarPorId(Connection c, int id) throws SQLException {
        List<OrdemServico> lista = consultar(c, SELECT + "WHERE o.id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Lista ordens filtrando opcionalmente por status e por termo (nº da OS, placa ou cliente). */
    public List<OrdemServico> listar(Connection c, StatusOrdem status, String termo) throws SQLException {
        String t = termo == null ? "" : termo.trim();
        String placa = t.toUpperCase().replaceAll("[\\s-]", "");
        String numero = t.replace("#", "");
        String sql = SELECT + "WHERE (? IS NULL OR o.status = ?) "
                + "AND (v.placa LIKE ? OR c.nome LIKE ? OR v.modelo LIKE ? OR CAST(o.id AS CHAR) = ?)"
                + ORDEM_PRIORIDADE;
        return consultar(c, sql, status, status, JdbcUtil.like(placa), JdbcUtil.like(t), JdbcUtil.like(t), numero);
    }

    public List<OrdemServico> listarPorVeiculo(Connection c, int veiculoId) throws SQLException {
        return consultar(c, SELECT + "WHERE o.veiculo_id = ? ORDER BY o.data_abertura DESC", veiculoId);
    }

    public List<OrdemServico> listarPorCliente(Connection c, int clienteId) throws SQLException {
        return consultar(c, SELECT + "WHERE o.cliente_id = ? ORDER BY o.data_abertura DESC", clienteId);
    }

    /** Ordens ainda não entregues, VIP primeiro (fila de atendimento). */
    public List<OrdemServico> listarFilaAtendimento(Connection c) throws SQLException {
        return consultar(c, SELECT + "WHERE o.status <> 'ENTREGUE'" + ORDEM_PRIORIDADE);
    }

    public List<OrdemServico> listarAguardandoPagamento(Connection c) throws SQLException {
        return consultar(c, SELECT + "WHERE o.status = 'PRONTO_PARA_ENTREGA' AND o.situacao_pagamento = 'PENDENTE'"
                + ORDEM_PRIORIDADE);
    }

    public int contarPorStatus(Connection c, StatusOrdem status) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM ordens_servico WHERE status = ?", status);
    }

    public int contarAguardandoPagamento(Connection c) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM ordens_servico WHERE status = 'PRONTO_PARA_ENTREGA' "
                + "AND situacao_pagamento = 'PENDENTE'");
    }

    private List<OrdemServico> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<OrdemServico> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private OrdemServico mapear(ResultSet rs) throws SQLException {
        OrdemServico o = new OrdemServico();
        o.setId(rs.getInt("id"));
        o.setVeiculoId(rs.getInt("veiculo_id"));
        o.setClienteId(rs.getInt("cliente_id"));
        o.setDescricao(rs.getString("descricao"));
        o.setHorasMaoObra(rs.getBigDecimal("horas_mao_obra"));
        o.setDataAbertura(JdbcUtil.dataHora(rs, "data_abertura"));
        o.setDataAgendada(JdbcUtil.data(rs, "data_agendada"));
        o.setStatus(StatusOrdem.valueOf(rs.getString("status")));
        o.setValorPecas(rs.getBigDecimal("valor_pecas"));
        o.setValorMaoObra(rs.getBigDecimal("valor_mao_obra"));
        o.setDesconto(rs.getBigDecimal("desconto"));
        o.setValorTotal(rs.getBigDecimal("valor_total"));
        o.setSituacaoPagamento(SituacaoPagamento.valueOf(rs.getString("situacao_pagamento")));
        o.setEstoqueBaixado(rs.getBoolean("estoque_baixado"));
        o.setDataConclusao(JdbcUtil.dataHora(rs, "data_conclusao"));
        o.setDataEntrega(JdbcUtil.dataHora(rs, "data_entrega"));
        o.setPlaca(rs.getString("placa"));
        o.setModeloVeiculo(rs.getString("modelo"));
        o.setClienteNome(rs.getString("cliente_nome"));
        o.setClienteCategoria(CategoriaCliente.valueOf(rs.getString("cliente_categoria")));
        return o;
    }
}
