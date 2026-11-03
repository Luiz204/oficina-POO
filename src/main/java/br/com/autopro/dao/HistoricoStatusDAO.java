package br.com.autopro.dao;

import br.com.autopro.model.HistoricoStatus;
import br.com.autopro.model.StatusOrdem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoricoStatusDAO {

    public void inserir(Connection c, HistoricoStatus h) throws SQLException {
        JdbcUtil.executar(c, "INSERT INTO historico_status (ordem_id, status_anterior, status_novo, data_hora, "
                + "observacao) VALUES (?, ?, ?, ?, ?)", h.getOrdemId(), h.getStatusAnterior(), h.getStatusNovo(),
                java.time.LocalDateTime.now().withNano(0), h.getObservacao());
    }

    public List<HistoricoStatus> listarPorOrdem(Connection c, int ordemId) throws SQLException {
        List<HistoricoStatus> lista = new ArrayList<>();
        String sql = "SELECT id, ordem_id, status_anterior, status_novo, data_hora, observacao "
                + "FROM historico_status WHERE ordem_id = ? ORDER BY data_hora, id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, ordemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistoricoStatus h = new HistoricoStatus();
                    h.setId(rs.getInt("id"));
                    h.setOrdemId(rs.getInt("ordem_id"));
                    h.setStatusAnterior(JdbcUtil.enumOuNulo(StatusOrdem.class, rs.getString("status_anterior")));
                    h.setStatusNovo(StatusOrdem.valueOf(rs.getString("status_novo")));
                    h.setDataHora(JdbcUtil.dataHora(rs, "data_hora"));
                    h.setObservacao(rs.getString("observacao"));
                    lista.add(h);
                }
            }
        }
        return lista;
    }
}
