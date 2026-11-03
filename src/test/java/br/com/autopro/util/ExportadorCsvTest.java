package br.com.autopro.util;

import br.com.autopro.model.Relatorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportadorCsvTest {

    @Test
    void escapaSeparadorAspasEQuebrasDeLinha() {
        assertEquals("simples", ExportadorCsv.escapar("simples"));
        assertEquals("\"a;b\"", ExportadorCsv.escapar("a;b"));
        assertEquals("\"diz \"\"oi\"\"\"", ExportadorCsv.escapar("diz \"oi\""));
        assertEquals("\"linha1\nlinha2\"", ExportadorCsv.escapar("linha1\nlinha2"));
        assertEquals("", ExportadorCsv.escapar(null));
    }

    @Test
    void geraArquivoComCabecalhoELinhas(@TempDir Path pasta) throws Exception {
        Relatorio r = new Relatorio("Teste", "OS", "Cliente", "Total");
        r.adicionarLinha(1, "João; Silva", "R$ 10,00");
        Path arquivo = pasta.resolve("r.csv");
        ExportadorCsv.exportar(r, arquivo);
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        assertEquals("﻿OS;Cliente;Total", linhas.get(0));
        assertEquals("1;\"João; Silva\";R$ 10,00", linhas.get(1));
    }
}
