package br.com.autopro.util;

import br.com.autopro.model.Relatorio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exporta relatórios para CSV (UTF-8 com BOM e separador ";"),
 * formato que abre corretamente no Excel/LibreOffice em português.
 */
public final class ExportadorCsv {

    private static final char SEPARADOR = ';';

    private ExportadorCsv() {
    }

    public static void exportar(Relatorio relatorio, Path destino) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            w.write(linha(relatorio.getColunas()));
            w.newLine();
            for (Object[] linha : relatorio.getLinhas()) {
                w.write(linha(java.util.Arrays.asList(linha)));
                w.newLine();
            }
        }
    }

    static String linha(List<?> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(escapar(valores.get(i)));
        }
        return sb.toString();
    }

    static String escapar(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        boolean precisaAspas = texto.indexOf(SEPARADOR) >= 0 || texto.indexOf('"') >= 0
                || texto.indexOf('\n') >= 0 || texto.indexOf('\r') >= 0;
        if (precisaAspas) {
            return '"' + texto.replace("\"", "\"\"") + '"';
        }
        return texto;
    }
}
