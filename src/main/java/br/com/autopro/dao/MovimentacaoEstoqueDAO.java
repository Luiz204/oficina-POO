package br.com.autopro.dao;

import br.com.autopro.model.MovimentacaoEstoque;
import br.com.autopro.model.TipoMovimentacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovimentacaoEstoqueDAO {

    private static final String SELECT = "SELECT m.id, m.peca_id, p.codigo, p.nome, m.tipo, m.quantidade, "
            + "m.preco_unitario, m.ordem_id, v.placa, m.data_hora, m.observacao "
            + "FROM movimentacoes_estoque m JOIN pecas p ON p.id = m.peca_id "
            + "LEFT JOIN ordens_servico o ON o.id = m.ordem_id LEFT JOIN veiculos v ON v.id = o.veiculo_id ";

    public int inserir(Connection c, MovimentacaoEstoque m) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO movimentacoes_estoque (peca_id, tipo, quantidade, preco_unitario, "
                        + "ordem_id, data_hora, observacao) VALUES (?, ?, ?, ?, ?, ?, ?)",
                m.getPecaId(), m.getTipo(), m.getQuantidade(), m.getPrecoUnitario(), m.getOrdemId(),
                java.time.LocalDateTime.now().withNano(0), m.getObservacao());
    }

    public List<MovimentacaoEstoque> listarPorPeca(Connection c, int pecaId) throws SQLException {
        return consultar(c, SELECT + "WHERE m.peca_id = ? ORDER BY m.data_hora DESC, m.id DESC", pecaId);
    }

    public List<MovimentacaoEstoque> listarPorOrdem(Connection c, int ordemId) throws SQLException {
        return consultar(c, SELECT + "WHERE m.ordem_id = ? ORDER BY m.id", ordemId);
    }

    /** Remove as entradas de uma peça nunca utilizada (usado somente na exclusão da peça). */
    public void excluirEntradasDaPeca(Connection c, int pecaId) throws SQLException {
        JdbcUtil.executar(c, "DELETE FROM movimentacoes_estoque WHERE peca_id = ? AND tipo = 'ENTRADA'", pecaId);
    }

    public int contarSaidasPorOrdem(Connection c, int ordemId) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM movimentacoes_estoque WHERE ordem_id = ? AND tipo = 'SAIDA'",
                ordemId);
    }

    private List<MovimentacaoEstoque> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<MovimentacaoEstoque> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimentacaoEstoque m = new MovimentacaoEstoque();
                    m.setId(rs.getInt("id"));
                    m.setPecaId(rs.getInt("peca_id"));
                    m.setPecaCodigo(rs.getString("codigo"));
                    m.setPecaNome(rs.getString("nome"));
                    m.setTipo(TipoMovimentacao.valueOf(rs.getString("tipo")));
                    m.setQuantidade(rs.getInt("quantidade"));
                    m.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    m.setOrdemId(JdbcUtil.inteiroOuNulo(rs, "ordem_id"));
                    m.setPlaca(rs.getString("placa"));
                    m.setDataHora(JdbcUtil.dataHora(rs, "data_hora"));
                    m.setObservacao(rs.getString("observacao"));
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
