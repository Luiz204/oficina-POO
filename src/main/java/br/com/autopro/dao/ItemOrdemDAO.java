package br.com.autopro.dao;

import br.com.autopro.model.ItemOrdem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemOrdemDAO {

    private static final String SELECT = "SELECT i.id, i.ordem_id, i.peca_id, p.codigo, p.nome, i.quantidade, "
            + "i.preco_unitario FROM itens_ordem i JOIN pecas p ON p.id = i.peca_id ";

    public List<ItemOrdem> listarPorOrdem(Connection c, int ordemId) throws SQLException {
        return consultar(c, SELECT + "WHERE i.ordem_id = ? ORDER BY i.peca_id", ordemId);
    }

    /** Peças efetivamente utilizadas (com baixa de estoque) nas ordens de um veículo. */
    public List<ItemOrdem> listarUtilizadasPorVeiculo(Connection c, int veiculoId) throws SQLException {
        return consultar(c, SELECT + "JOIN ordens_servico o ON o.id = i.ordem_id "
                + "WHERE o.veiculo_id = ? AND o.estoque_baixado = TRUE ORDER BY o.data_abertura DESC, i.id", veiculoId);
    }

    /** Substitui todos os itens da ordem pelos informados (usado enquanto o estoque não foi baixado). */
    public void substituirItens(Connection c, int ordemId, List<ItemOrdem> itens) throws SQLException {
        JdbcUtil.executar(c, "DELETE FROM itens_ordem WHERE ordem_id = ?", ordemId);
        String sql = "INSERT INTO itens_ordem (ordem_id, peca_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (ItemOrdem item : itens) {
                JdbcUtil.parametros(ps, ordemId, item.getPecaId(), item.getQuantidade(), item.getPrecoUnitario());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<ItemOrdem> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<ItemOrdem> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemOrdem i = new ItemOrdem(rs.getInt("peca_id"), rs.getString("codigo"), rs.getString("nome"),
                            rs.getInt("quantidade"), rs.getBigDecimal("preco_unitario"));
                    i.setId(rs.getInt("id"));
                    i.setOrdemId(rs.getInt("ordem_id"));
                    lista.add(i);
                }
            }
        }
        return lista;
    }
}
