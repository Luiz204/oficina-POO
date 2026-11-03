package br.com.autopro.dao;

import br.com.autopro.model.FormaPagamento;
import br.com.autopro.model.Pagamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PagamentoDAO {

    private static final String SELECT = "SELECT p.id, p.ordem_id, p.valor, p.forma, p.data_hora, p.situacao, "
            + "p.observacao, c.nome AS cliente_nome, v.placa FROM pagamentos p "
            + "JOIN ordens_servico o ON o.id = p.ordem_id JOIN clientes c ON c.id = o.cliente_id "
            + "JOIN veiculos v ON v.id = o.veiculo_id ";

    public int inserir(Connection c, Pagamento p) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO pagamentos (ordem_id, valor, forma, data_hora, observacao) "
                        + "VALUES (?, ?, ?, ?, ?)", p.getOrdemId(), p.getValor(), p.getForma(),
                java.time.LocalDateTime.now().withNano(0), p.getObservacao());
    }

    public boolean existeParaOrdem(Connection c, int ordemId) throws SQLException {
        return JdbcUtil.existe(c, "SELECT COUNT(*) FROM pagamentos WHERE ordem_id = ?", ordemId);
    }

    public Optional<Pagamento> buscarPorOrdem(Connection c, int ordemId) throws SQLException {
        List<Pagamento> lista = consultar(c, SELECT + "WHERE p.ordem_id = ?", ordemId);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Pagamentos filtrados por cliente, placa ou nº da OS. */
    public List<Pagamento> listar(Connection c, String termo) throws SQLException {
        String t = termo == null ? "" : termo.trim();
        String placa = t.toUpperCase().replaceAll("[\\s-]", "");
        return consultar(c, SELECT + "WHERE c.nome LIKE ? OR v.placa LIKE ? OR CAST(p.ordem_id AS CHAR) = ? "
                + "ORDER BY p.data_hora DESC", JdbcUtil.like(t), JdbcUtil.like(placa), t.replace("#", ""));
    }

    private List<Pagamento> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<Pagamento> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pagamento p = new Pagamento();
                    p.setId(rs.getInt("id"));
                    p.setOrdemId(rs.getInt("ordem_id"));
                    p.setValor(rs.getBigDecimal("valor"));
                    p.setForma(FormaPagamento.valueOf(rs.getString("forma")));
                    p.setDataHora(JdbcUtil.dataHora(rs, "data_hora"));
                    p.setSituacao(rs.getString("situacao"));
                    p.setObservacao(rs.getString("observacao"));
                    p.setClienteNome(rs.getString("cliente_nome"));
                    p.setPlaca(rs.getString("placa"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }
}
