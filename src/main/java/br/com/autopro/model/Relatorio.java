package br.com.autopro.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Resultado tabular de um relatório: colunas, linhas já formatadas e um resumo opcional. */
public class Relatorio {

    private final String titulo;
    private final List<String> colunas;
    private final List<Object[]> linhas = new ArrayList<>();
    private String resumo;

    public Relatorio(String titulo, String... colunas) {
        this.titulo = titulo;
        this.colunas = Collections.unmodifiableList(Arrays.asList(colunas));
    }

    public void adicionarLinha(Object... valores) {
        if (valores.length != colunas.size()) {
            throw new IllegalArgumentException("Quantidade de valores difere da quantidade de colunas");
        }
        linhas.add(valores);
    }

    public String getTitulo() { return titulo; }
    public List<String> getColunas() { return colunas; }
    public List<Object[]> getLinhas() { return linhas; }
    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }
}
