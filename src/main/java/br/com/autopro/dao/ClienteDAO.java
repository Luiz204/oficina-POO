package br.com.autopro.dao;

import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO {

    private static final String SELECT = "SELECT id, nome, telefone, email, categoria FROM clientes ";

    public int inserir(Connection c, Cliente cliente) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO clientes (nome, telefone, email, categoria) VALUES (?, ?, ?, ?)",
                cliente.getNome(), cliente.getTelefone(), cliente.getEmail(), cliente.getCategoria());
    }

    public void atualizar(Connection c, Cliente cliente) throws SQLException {
        JdbcUtil.executar(c, "UPDATE clientes SET nome = ?, telefone = ?, email = ?, categoria = ? WHERE id = ?",
                cliente.getNome(), cliente.getTelefone(), cliente.getEmail(), cliente.getCategoria(), cliente.getId());
    }

    public boolean excluir(Connection c, int id) throws SQLException {
        return JdbcUtil.executar(c, "DELETE FROM clientes WHERE id = ?", id) > 0;
    }

    public Optional<Cliente> buscarPorId(Connection c, int id) throws SQLException {
        List<Cliente> lista = consultar(c, SELECT + "WHERE id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Pesquisa por nome ou telefone (comparando apenas os dígitos). */
    public List<Cliente> listar(Connection c, String termo, boolean somenteVip) throws SQLException {
        String digitos = JdbcUtil.somenteDigitos(termo);
        String sql = SELECT
                + "WHERE (nome LIKE ? OR (? <> '' AND REGEXP_REPLACE(telefone, '[^0-9]', '') LIKE ?)) "
                + (somenteVip ? "AND categoria = 'VIP' " : "")
                + "ORDER BY nome";
        return consultar(c, sql, JdbcUtil.like(termo), digitos, "%" + digitos + "%");
    }

    public int contar(Connection c) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM clientes");
    }

    public boolean possuiVeiculos(Connection c, int id) throws SQLException {
        return JdbcUtil.existe(c, "SELECT COUNT(*) FROM veiculos WHERE cliente_id = ?", id);
    }

    public boolean possuiOrdens(Connection c, int id) throws SQLException {
        return JdbcUtil.existe(c, "SELECT COUNT(*) FROM ordens_servico WHERE cliente_id = ?", id);
    }

    private List<Cliente> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Cliente(rs.getInt("id"), rs.getString("nome"), rs.getString("telefone"),
                            rs.getString("email"), CategoriaCliente.valueOf(rs.getString("categoria"))));
                }
            }
        }
        return lista;
    }
}
