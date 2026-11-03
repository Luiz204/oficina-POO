package br.com.autopro.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ConfiguracaoDAO {

    public Optional<String> obter(Connection c, String chave) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT valor FROM configuracoes WHERE chave = ?")) {
            JdbcUtil.parametros(ps, chave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public void salvar(Connection c, String chave, String valor) throws SQLException {
        JdbcUtil.executar(c, "INSERT INTO configuracoes (chave, valor) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE valor = VALUES(valor)", chave, valor);
    }
}
