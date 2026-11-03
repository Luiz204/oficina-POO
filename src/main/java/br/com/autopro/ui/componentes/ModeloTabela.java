package br.com.autopro.ui.componentes;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Modelo de tabela genérico e somente leitura: cada coluna é definida por um título,
 * um tipo (usado para ordenação e formatação) e uma função que extrai o valor do objeto.
 */
public class ModeloTabela<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final class Coluna<T> {
        private final String titulo;
        private final Class<?> tipo;
        private final Function<T, Object> extrator;

        private Coluna(String titulo, Class<?> tipo, Function<T, Object> extrator) {
            this.titulo = titulo;
            this.tipo = tipo;
            this.extrator = extrator;
        }
    }

    private final List<Coluna<T>> colunas = new ArrayList<>();
    private List<T> dados = new ArrayList<>();

    public ModeloTabela<T> coluna(String titulo, Function<T, Object> extrator) {
        return coluna(titulo, Object.class, extrator);
    }

    public ModeloTabela<T> coluna(String titulo, Class<?> tipo, Function<T, Object> extrator) {
        colunas.add(new Coluna<>(titulo, tipo, extrator));
        return this;
    }

    public void setDados(List<T> novos) {
        this.dados = new ArrayList<>(novos);
        fireTableDataChanged();
    }

    public List<T> getDados() {
        return dados;
    }

    public T getItem(int linhaModelo) {
        return linhaModelo >= 0 && linhaModelo < dados.size() ? dados.get(linhaModelo) : null;
    }

    @Override
    public int getRowCount() {
        return dados.size();
    }

    @Override
    public int getColumnCount() {
        return colunas.size();
    }

    @Override
    public String getColumnName(int coluna) {
        return colunas.get(coluna).titulo;
    }

    @Override
    public Class<?> getColumnClass(int coluna) {
        return colunas.get(coluna).tipo;
    }

    @Override
    public Object getValueAt(int linha, int coluna) {
        return colunas.get(coluna).extrator.apply(dados.get(linha));
    }
}
