package br.com.autopro.dao;

import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.Veiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VeiculoDAO {

    private static final String SELECT = "SELECT v.id, v.cliente_id, v.modelo, v.ano, v.placa, v.km_atual, "
            + "v.data_ultima_revisao, v.km_ultima_revisao, c.nome AS cliente_nome, c.categoria AS cliente_categoria "
            + "FROM veiculos v JOIN clientes c ON c.id = v.cliente_id ";

    public int inserir(Connection c, Veiculo v) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO veiculos (cliente_id, modelo, ano, placa, km_atual, "
                        + "data_ultima_revisao, km_ultima_revisao) VALUES (?, ?, ?, ?, ?, ?, ?)",
                v.getClienteId(), v.getModelo(), v.getAno(), v.getPlaca(), v.getKmAtual(),
                v.getDataUltimaRevisao(), v.getKmUltimaRevisao());
    }

    /** Atualiza os dados cadastrais (os dados de revisão são atualizados pelo módulo de revisões). */
    public void atualizar(Connection c, Veiculo v) throws SQLException {
        JdbcUtil.executar(c, "UPDATE veiculos SET cliente_id = ?, modelo = ?, ano = ?, placa = ?, km_atual = ? "
                        + "WHERE id = ?",
                v.getClienteId(), v.getModelo(), v.getAno(), v.getPlaca(), v.getKmAtual(), v.getId());
    }

    public void atualizarKm(Connection c, int id, int km) throws SQLException {
        JdbcUtil.executar(c, "UPDATE veiculos SET km_atual = ? WHERE id = ?", km, id);
    }

    public void atualizarUltimaRevisao(Connection c, int id, LocalDate data, int km) throws SQLException {
        JdbcUtil.executar(c, "UPDATE veiculos SET data_ultima_revisao = ?, km_ultima_revisao = ? WHERE id = ?",
                data, km, id);
    }

    public boolean excluir(Connection c, int id) throws SQLException {
        return JdbcUtil.executar(c, "DELETE FROM veiculos WHERE id = ?", id) > 0;
    }

    public Optional<Veiculo> buscarPorId(Connection c, int id) throws SQLException {
        List<Veiculo> lista = consultar(c, SELECT + "WHERE v.id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Bloqueia a linha do veículo até o fim da transação (SELECT ... FOR UPDATE). */
    public void bloquear(Connection c, int id) throws SQLException {
        JdbcUtil.contar(c, "SELECT id FROM veiculos WHERE id = ? FOR UPDATE", id);
    }

    /** Pesquisa por placa, modelo ou nome do proprietário. */
    public List<Veiculo> listar(Connection c, String termo) throws SQLException {
        String placa = termo == null ? "" : termo.toUpperCase().replaceAll("[\\s-]", "");
        return consultar(c, SELECT + "WHERE v.placa LIKE ? OR v.modelo LIKE ? OR c.nome LIKE ? ORDER BY v.placa",
                JdbcUtil.like(placa), JdbcUtil.like(termo), JdbcUtil.like(termo));
    }

    public List<Veiculo> listarPorCliente(Connection c, int clienteId) throws SQLException {
        return consultar(c, SELECT + "WHERE v.cliente_id = ? ORDER BY v.placa", clienteId);
    }

    public boolean existePlaca(Connection c, String placa, Integer ignorarId) throws SQLException {
        return JdbcUtil.existe(c, "SELECT COUNT(*) FROM veiculos WHERE placa = ? AND id <> ?",
                placa, ignorarId == null ? 0 : ignorarId);
    }

    public int contar(Connection c) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM veiculos");
    }

    public boolean possuiVinculos(Connection c, int id) throws SQLException {
        return JdbcUtil.existe(c, "SELECT (SELECT COUNT(*) FROM ordens_servico WHERE veiculo_id = ?) "
                + "+ (SELECT COUNT(*) FROM revisoes WHERE veiculo_id = ?) "
                + "+ (SELECT COUNT(*) FROM agendamentos WHERE veiculo_id = ?)", id, id, id);
    }

    private List<Veiculo> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<Veiculo> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Veiculo v = new Veiculo();
                    v.setId(rs.getInt("id"));
                    v.setClienteId(rs.getInt("cliente_id"));
                    v.setModelo(rs.getString("modelo"));
                    v.setAno(rs.getInt("ano"));
                    v.setPlaca(rs.getString("placa"));
                    v.setKmAtual(rs.getInt("km_atual"));
                    v.setDataUltimaRevisao(JdbcUtil.data(rs, "data_ultima_revisao"));
                    v.setKmUltimaRevisao(JdbcUtil.inteiroOuNulo(rs, "km_ultima_revisao"));
                    v.setClienteNome(rs.getString("cliente_nome"));
                    v.setClienteCategoria(CategoriaCliente.valueOf(rs.getString("cliente_categoria")));
                    lista.add(v);
                }
            }
        }
        return lista;
    }
}
