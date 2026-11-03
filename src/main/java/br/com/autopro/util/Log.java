package br.com.autopro.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Configuração simples de log em arquivo (logs/autopro.log). Nunca registre senhas. */
public final class Log {

    private static final String RAIZ = "br.com.autopro";
    private static boolean configurado;

    private Log() {
    }

    public static synchronized void configurar() {
        if (configurado) {
            return;
        }
        configurado = true;
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$td/%1$tm/%1$tY %1$tH:%1$tM:%1$tS %4$s %2$s - %5$s%6$s%n");
        Logger raiz = Logger.getLogger(RAIZ);
        try {
            Path pasta = Paths.get("logs");
            Files.createDirectories(pasta);
            FileHandler arquivo = new FileHandler(pasta.resolve("autopro.log").toString(), 1_000_000, 3, true);
            arquivo.setEncoding("UTF-8");
            arquivo.setFormatter(new SimpleFormatter());
            raiz.addHandler(arquivo);
        } catch (IOException e) {
            raiz.log(Level.WARNING, "Não foi possível criar o arquivo de log", e);
        }
    }

    public static Logger get(Class<?> classe) {
        return Logger.getLogger(classe.getName());
    }
}
