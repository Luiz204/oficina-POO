package br.com.autopro.dao;

import br.com.autopro.model.Agendamento;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.StatusAgendamento;
import br.com.autopro.model.TipoAgendamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgendamentoDAO {

    private static final String SELECT = "SELECT a.id, a.veiculo_id, a.data_hora, a.tipo, a.descricao, "
            + "a.duracao_minutos, a.status, a.ordem_id, v.placa, v.modelo, c.id AS cliente_id, "
            + "c.nome AS cliente_nome, c.telefone AS cliente_telefone, c.categoria AS cliente_categoria "
            + "FROM agendamentos a JOIN veiculos v ON v.id = a.veiculo_id JOIN clientes c ON c.id = v.cliente_id ";

    /** Em horários iguais, clientes VIP aparecem primeiro. */
    private static final String ORDENACAO = " ORDER BY a.data_hora, (c.categoria = 'VIP') DESC";

    public int inserir(Connection c, Agendamento a) throws SQLException {
        return JdbcUtil.inserir(c, "INSERT INTO agendamentos (veiculo_id, data_hora, tipo, descricao, duracao_minutos, "
                        + "status, ordem_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                a.getVeiculoId(), a.getDataHora(), a.getTipo(), a.getDescricao(), a.getDuracaoMinutos(),
                a.getStatus(), a.getOrdemId());
    }

    public void atualizar(Connection c, Agendamento a) throws SQLException {
        JdbcUtil.executar(c, "UPDATE agendamentos SET veiculo_id = ?, data_hora = ?, tipo = ?, descricao = ?, "
                        + "duracao_minutos = ?, status = ?, ordem_id = ? WHERE id = ?",
                a.getVeiculoId(), a.getDataHora(), a.getTipo(), a.getDescricao(), a.getDuracaoMinutos(),
                a.getStatus(), a.getOrdemId(), a.getId());
    }

    public Optional<Agendamento> buscarPorId(Connection c, int id) throws SQLException {
        List<Agendamento> lista = consultar(c, SELECT + "WHERE a.id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    /** Agendamentos com status AGENDADO a partir de um instante. */
    public List<Agendamento> listarFuturos(Connection c, LocalDateTime aPartirDe) throws SQLException {
        return consultar(c, SELECT + "WHERE a.status = 'AGENDADO' AND a.data_hora >= ?" + ORDENACAO, aPartirDe);
    }

    public List<Agendamento> listar(Connection c, StatusAgendamento status) throws SQLException {
        return consultar(c, SELECT + "WHERE (? IS NULL OR a.status = ?)" + ORDENACAO, status, status);
    }

    public int contarFuturos(Connection c, LocalDateTime de, LocalDateTime ate) throws SQLException {
        return JdbcUtil.contar(c, "SELECT COUNT(*) FROM agendamentos WHERE status = 'AGENDADO' "
                + "AND data_hora >= ? AND data_hora < ?", de, ate);
    }

    private List<Agendamento> consultar(Connection c, String sql, Object... params) throws SQLException {
        List<Agendamento> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcUtil.parametros(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Agendamento a = new Agendamento();
                    a.setId(rs.getInt("id"));
                    a.setVeiculoId(rs.getInt("veiculo_id"));
                    a.setDataHora(JdbcUtil.dataHora(rs, "data_hora"));
                    a.setTipo(TipoAgendamento.valueOf(rs.getString("tipo")));
                    a.setDescricao(rs.getString("descricao"));
                    a.setDuracaoMinutos(JdbcUtil.inteiroOuNulo(rs, "duracao_minutos"));
                    a.setStatus(StatusAgendamento.valueOf(rs.getString("status")));
                    a.setOrdemId(JdbcUtil.inteiroOuNulo(rs, "ordem_id"));
                    a.setPlaca(rs.getString("placa"));
                    a.setModeloVeiculo(rs.getString("modelo"));
                    a.setClienteId(rs.getInt("cliente_id"));
                    a.setClienteNome(rs.getString("cliente_nome"));
                    a.setClienteTelefone(rs.getString("cliente_telefone"));
                    a.setClienteCategoria(CategoriaCliente.valueOf(rs.getString("cliente_categoria")));
                    lista.add(a);
                }
            }
        }
        return lista;
    }
}
