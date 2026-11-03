package br.com.autopro.ui.componentes;

import javax.swing.JScrollPane;
import java.awt.Color;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/** Combina {@link ModeloTabela} e {@link TabelaPadrao} para trabalhar diretamente com objetos. */
public class Tabela<T> {

    private final ModeloTabela<T> modelo;
    private final TabelaPadrao tabela;

    public Tabela(ModeloTabela<T> modelo) {
        this.modelo = modelo;
        this.tabela = new TabelaPadrao(modelo);
    }

    public void setDados(List<T> dados) {
        modelo.setDados(dados);
    }

    public T getSelecionado() {
        return modelo.getItem(tabela.getLinhaModeloSelecionada());
    }

    /** Destaca com a cor informada as linhas que satisfazem a condição. */
    public Tabela<T> destacar(Predicate<T> condicao, Color cor) {
        Function<T, Color> anterior = corDestaque;
        corDestaque = item -> condicao.test(item) ? cor : (anterior != null ? anterior.apply(item) : null);
        tabela.setDestaque(i -> corDestaque.apply(modelo.getItem(i)));
        return this;
    }

    public Tabela<T> corTexto(Function<T, Color> cor) {
        tabela.setCorTexto(i -> cor.apply(modelo.getItem(i)));
        return this;
    }

    private Function<T, Color> corDestaque;

    public Tabela<T> larguras(int... larguras) {
        tabela.larguras(larguras);
        return this;
    }

    public void aoDuploClique(Runnable acao) {
        tabela.aoDuploClique(acao);
    }

    public void aoSelecionar(Runnable acao) {
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                acao.run();
            }
        });
    }

    public TabelaPadrao getTabela() {
        return tabela;
    }

    public List<T> getDados() {
        return modelo.getDados();
    }

    public JScrollPane comRolagem() {
        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(Tema.BORDA));
        return sp;
    }
}
