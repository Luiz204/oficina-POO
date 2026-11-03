package br.com.autopro.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Fábrica de conexões JDBC.
 * <p>
 * As credenciais são lidas, nesta ordem (a última vence):
 * <ol>
 *   <li>{@code src/main/resources/db.properties} (embutido no JAR);</li>
 *   <li>arquivo {@code db.properties} na pasta onde a aplicação é executada
 *       (ou o caminho indicado em {@code -Dautopro.config=...});</li>
 *   <li>variáveis de ambiente {@code AUTOPRO_DB_URL}, {@code AUTOPRO_DB_USUARIO}, {@code AUTOPRO_DB_SENHA}.</li>
 * </ol>
 */
public final class ConexaoBD {

    private static final Logger LOG = Log.get(ConexaoBD.class);
    private static Properties propriedades;

    private ConexaoBD() {
    }

    private static synchronized Properties carregar() {
        if (propriedades != null) {
            return propriedades;
        }
        Properties p = new Properties();
        try (InputStream in = ConexaoBD.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOG.warning("Falha ao ler db.properties interno: " + e.getMessage());
        }
        Path externo = Paths.get(System.getProperty("autopro.config", "db.properties"));
        if (Files.isRegularFile(externo)) {
            try (Reader r = Files.newBufferedReader(externo, StandardCharsets.UTF_8)) {
                p.load(r);
                LOG.info("Configuração de banco lida de " + externo.toAbsolutePath());
            } catch (IOException e) {
                LOG.warning("Falha ao ler " + externo + ": " + e.getMessage());
            }
        }
        sobrescreverComAmbiente(p, "AUTOPRO_DB_URL", "db.url");
        sobrescreverComAmbiente(p, "AUTOPRO_DB_USUARIO", "db.usuario");
        sobrescreverComAmbiente(p, "AUTOPRO_DB_SENHA", "db.senha");
        propriedades = p;
        return p;
    }

    private static void sobrescreverComAmbiente(Properties p, String variavel, String chave) {
        String valor = System.getenv(variavel);
        if (valor != null && !valor.isBlank()) {
            p.setProperty(chave, valor);
        }
    }

    /** Permite substituir a configuração (usado pelos testes de integração). */
    public static synchronized void configurar(Properties p) {
        propriedades = p;
    }

    public static Connection getConnection() throws SQLException {
        Properties p = carregar();
        String url = p.getProperty("db.url");
        if (url == null || url.isBlank()) {
            throw new SQLException("Propriedade db.url não configurada em db.properties", "08001");
        }
        return DriverManager.getConnection(url, p.getProperty("db.usuario", ""), p.getProperty("db.senha", ""));
    }

    /** Descrição da conexão para exibição/log, sem usuário e senha. */
    public static String descricao() {
        String url = carregar().getProperty("db.url", "(não configurado)");
        int i = url.indexOf('?');
        return i >= 0 ? url.substring(0, i) : url;
    }
}
