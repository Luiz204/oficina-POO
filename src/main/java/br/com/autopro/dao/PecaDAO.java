package br.com.autopro.dao;

import br.com.autopro.model.Peca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PecaDAO {

    private static final String SELECT = "SELECT id, codigo, nome, descricao, preco_unitario, quantidade, "
            + "estoque_minimo FROM pecas ";

    public int inserir(Connection c, Peca p) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO pecas (codigo, nome, descricao, preco_unitario, quantidade, "
                        + "estoque_minimo) VALUES (?, ?, ?, ?, ?, ?)",
                p.getCodigo(), p.getNome(), p.getDescricao(), p.getPrecoUnitario(), p.getQuantidade(),
                p.getEstoqueMinimo());
    }

    public void atualizar(Connection c, Peca p) throws SQLException {
        JdbcUtil.executar(c, "UPDATE pecas SET codigo = ?, nome = ?, descricao = ?, preco_unitario = ?, "
                        + "quantidade = ?, estoque_minimo = ? WHERE id = ?",
                p.getCodigo(), p.getNome(), p.getDescricao(), p.getPrecoUnitario(), p.getQuantidade(),
                p.getEstoqueMinimo(), p.getId());
    }

    public boolean excluir(Connection c, int id) throws SQLException {
        return JdbcUtil.executar(c, "DELETE FROM pecas WHERE id = ?", id) > 0;
    }

    public Optional<Peca> buscarPorId(Connection c, int id) throws SQLException {
        List<Peca> lista = consultar(c, SELECT + "WHERE id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Lê a peça bloqueando a linha até o fim da transação (evita baixas concorrentes). */
    public Optional<Peca> buscarParaAtualizacao(Connection c, int id) throws SQLException {
        List<Peca> lista = consultar(c, SELECT + "WHERE id = ? FOR UPDATE", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    public List<Peca> listar(Connection c, String termo, boolean somenteAbaixoMinimo) throws SQLException {
        String sql = SELECT + "WHERE (codigo LIKE ? OR nome LIKE ?) "
                + (somenteAbaixoMinimo ? "AND quantidade < estoque_minimo " : "")
                + "ORDER BY nome";
        return consultar(c, sql, JdbcUtil.like(termo), JdbcUtil.like(termo));
    }

    public List<Peca> listarAbaixoMinimo(Connection c) throws SQLException {
        return consultar(c, SELECT + "WHERE quantidade < estoque_minimo ORDER BY nome");
    }

    public int contarAbaixoMinimo(Connection c) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM pecas WHERE quantidade < estoque_minimo");
    }

    public boolean existeCodigo(Connection c, String codigo, Integer ignorarId) throws SQLException {
        return JdbcUtil.existe(c, "SELECT COUNT(*) FROM pecas WHERE codigo = ? AND id <> ?",
                codigo, ignorarId == null ? 0 : ignorarId);
    }

    public void adicionarQuantidade(Connection c, int id, int quantidade) throws SQLException {
        JdbcUtil.executar(c, "UPDATE pecas SET quantidade = quantidade + ? WHERE id = ?", quantidade, id);
    }

    /**
     * Baixa a quantidade somente se houver saldo suficiente.
     * @return false se o estoque for insuficiente (nenhuma linha alterada)
     */
    public boolean baixarQuantidade(Connection c, int id, int quantidade) throws SQLException {
        return JdbcUtil.executar(c, "UPDATE pecas SET quantidade = quantidade - ? WHERE id = ? AND quantidade >= ?",
                quantidade, id, quantidade) == 1;
    }

    public boolean possuiVinculos(Connection c, int id) throws SQLException {
        return JdbcUtil.existe(c, "SELECT (SELECT COUNT(*) FROM itens_ordem WHERE peca_id = ?) "
                + "+ (SELECT COUNT(*) FROM movimentacoes_estoque WHERE peca_id = ? AND tipo = 'SAIDA')", id, id);
    }

    private List<Peca> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<Peca> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Peca p = new Peca();
                    p.setId(rs.getInt("id"));
                    p.setCodigo(rs.getString("codigo"));
                    p.setNome(rs.getString("nome"));
                    p.setDescricao(rs.getString("descricao"));
                    p.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    p.setQuantidade(rs.getInt("quantidade"));
                    p.setEstoqueMinimo(rs.getInt("estoque_minimo"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }
}
