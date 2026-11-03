package br.com.autopro.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executa operações de banco com gerenciamento automático de conexão e transação.
 * Erros de SQL são traduzidos em exceções compreensíveis ao usuário.
 */
public final class Transacao {

    private static final Logger LOG = Log.get(Transacao.class);

    @FunctionalInterface
    public interface Operacao<T> {
        T executar(Connection conexao) throws SQLException;
    }

    @FunctionalInterface
    public interface OperacaoSemRetorno {
        void executar(Connection conexao) throws SQLException;
    }

    private Transacao() {
    }

    /** Executa uma operação de leitura (auto-commit). */
    public static <T> T consultar(Operacao<T> operacao) {
        try (Connection c = ConexaoBD.getConnection()) {
            return operacao.executar(c);
        } catch (SQLException e) {
            throw ErrosBD.traduzir(e);
        }
    }

    /** Executa uma operação dentro de uma transação: commit em caso de sucesso, rollback em caso de erro. */
    public static <T> T executar(Operacao<T> operacao) {
        try (Connection c = ConexaoBD.getConnection()) {
            c.setAutoCommit(false);
            try {
                T resultado = operacao.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                desfazer(c);
                throw e;
            }
        } catch (SQLException e) {
            throw ErrosBD.traduzir(e);
        }
    }

    public static void executar(OperacaoSemRetorno operacao) {
        executar(c -> {
            operacao.executar(c);
            return null;
        });
    }

    private static void desfazer(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Falha ao executar rollback", ex);
        }
    }
}
