package br.com.autopro.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Funções auxiliares compartilhadas pelos DAOs. */
final class JdbcUtil {

    private JdbcUtil() {
    }

    /** Define os parâmetros do PreparedStatement, tratando nulos, enums e datas. */
    static void parametros(PreparedStatement ps, Object... valores) throws SQLException {
        for (int i = 0; i < valores.length; i++) {
            Object v = valores[i];
            int pos = i + 1;
            if (v == null) {
                ps.setNull(pos, Types.NULL);
            } else if (v instanceof Enum<?>) {
                ps.setString(pos, ((Enum<?>) v).name());
            } else if (v instanceof LocalDate || v instanceof LocalDateTime) {
                ps.setObject(pos, v);
            } else if (v instanceof BigDecimal) {
                ps.setBigDecimal(pos, (BigDecimal) v);
            } else if (v instanceof Boolean) {
                ps.setBoolean(pos, (Boolean) v);
            } else if (v instanceof Integer) {
                ps.setInt(pos, (Integer) v);
            } else {
                ps.setObject(pos, v);
            }
        }
    }

    static int executar(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            parametros(ps, valores);
            return ps.executeUpdate();
        }
    }

    static int inserir(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            parametros(ps, valores);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Nenhum ID gerado na inserção");
    }

    static int contar(Connection c, String sql, Object... valores) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            parametros(ps, valores);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    static boolean existe(Connection c, String sql, Object... valores) throws SQLException {
        return contar(c, sql, valores) > 0;
    }

    static Integer inteiroOuNulo(ResultSet rs, String coluna) throws SQLException {
        int v = rs.getInt(coluna);
        return rs.wasNull() ? null : v;
    }

    static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, LocalDate.class);
    }

    static LocalDateTime dataHora(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, LocalDateTime.class);
    }

    /** Monta o padrão para LIKE, escapando caracteres curinga digitados pelo usuário. */
    static String like(String termo) {
        String t = termo == null ? "" : termo.trim();
        t = t.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + t + "%";
    }

    static String somenteDigitos(String termo) {
        return termo == null ? "" : termo.replaceAll("\\D", "");
    }

    static <E extends Enum<E>> E enumOuNulo(Class<E> tipo, String valor) {
        return valor == null ? null : Enum.valueOf(tipo, valor);
    }
}
