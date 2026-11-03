package br.com.autopro.dao;

import br.com.autopro.model.Revisao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RevisaoDAO {

    private static final String SELECT = "SELECT r.id, r.veiculo_id, r.data_revisao, r.km, r.observacoes, v.placa "
            + "FROM revisoes r JOIN veiculos v ON v.id = r.veiculo_id ";

    public int inserir(Connection c, Revisao r) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO revisoes (veiculo_id, data_revisao, km, observacoes) VALUES (?, ?, ?, ?)",
                r.getVeiculoId(), r.getDataRevisao(), r.getKm(), r.getObservacoes());
    }

    public List<Revisao> listarPorVeiculo(Connection c, int veiculoId) throws SQLException {
        return consultar(c, SELECT + "WHERE r.veiculo_id = ? ORDER BY r.data_revisao DESC, r.id DESC", veiculoId);
    }

    /** Revisão mais recente do veículo (por data e, em empate, pela maior quilometragem). */
    public Optional<Revisao> buscarMaisRecente(Connection c, int veiculoId) throws SQLException {
        List<Revisao> lista = consultar(c, SELECT + "WHERE r.veiculo_id = ? ORDER BY r.data_revisao DESC, r.km DESC "
                + "LIMIT 1", veiculoId);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private List<Revisao> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<Revisao> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Revisao r = new Revisao(rs.getInt("veiculo_id"), JdbcUtil.data(rs, "data_revisao"),
                            rs.getInt("km"), rs.getString("observacoes"));
                    r.setId(rs.getInt("id"));
                    r.setPlaca(rs.getString("placa"));
                    lista.add(r);
                }
            }
        }
        return lista;
    }
}
