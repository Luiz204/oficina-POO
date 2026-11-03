package br.com.autopro.ui.componentes;

import br.com.autopro.model.StatusOrdem;
import br.com.autopro.util.Formatador;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.IntFunction;

/** JTable com ordenação, linhas alternadas, destaque por linha e formatação brasileira. */
public class TabelaPadrao extends JTable {

    private static final long serialVersionUID = 1L;

    private transient IntFunction<Color> destaque;
    private transient IntFunction<Color> corTexto;

    public TabelaPadrao(TableModel modelo) {
        super(modelo);
        setAutoCreateRowSorter(true);
        setRowHeight(30);
        setFillsViewportHeight(true);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(0, 1));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setFont(Tema.fonte(java.awt.Font.BOLD, 13));
        setFont(Tema.fonte(java.awt.Font.PLAIN, 13));

        setDefaultRenderer(BigDecimal.class, renderizador(v -> Formatador.moeda((BigDecimal) v), SwingConstants.RIGHT));
        setDefaultRenderer(LocalDate.class, renderizador(v -> Formatador.data((LocalDate) v), SwingConstants.CENTER));
        setDefaultRenderer(LocalDateTime.class,
                renderizador(v -> Formatador.dataHora((LocalDateTime) v), SwingConstants.CENTER));
        setDefaultRenderer(Integer.class, renderizador(String::valueOf, SwingConstants.RIGHT));
        setDefaultRenderer(StatusOrdem.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int l, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foco, l, c);
                comp.setFont(Tema.fonte(java.awt.Font.BOLD, 13));
                if (!sel) {
                    comp.setForeground(Tema.corStatus((StatusOrdem) v));
                }
                return comp;
            }
        });
    }

    private static DefaultTableCellRenderer renderizador(java.util.function.Function<Object, String> formato,
                                                         int alinhamento) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object valor) {
                setText(valor == null ? "" : formato.apply(valor));
            }
        };
        r.setHorizontalAlignment(alinhamento);
        return r;
    }

    /** Define uma cor de fundo por linha do modelo (null = cor padrão). */
    public void setDestaque(IntFunction<Color> destaque) {
        this.destaque = destaque;
        repaint();
    }

    /** Define uma cor de texto por linha do modelo (null = cor padrão). */
    public void setCorTexto(IntFunction<Color> corTexto) {
        this.corTexto = corTexto;
        repaint();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int linha, int coluna) {
        Component c = super.prepareRenderer(renderer, linha, coluna);
        if (!isRowSelected(linha)) {
            int linhaModelo = convertRowIndexToModel(linha);
            Color fundo = destaque != null ? destaque.apply(linhaModelo) : null;
            c.setBackground(fundo != null ? fundo : (linha % 2 == 0 ? getBackground() : Tema.LINHA_ALTERNADA));
            Color texto = corTexto != null ? corTexto.apply(linhaModelo) : null;
            if (getColumnClass(coluna) != StatusOrdem.class) {
                c.setForeground(texto != null ? texto : getForeground());
            }
        }
        return c;
    }

    /** Índice no modelo da linha selecionada, ou -1. */
    public int getLinhaModeloSelecionada() {
        int linha = getSelectedRow();
        return linha < 0 ? -1 : convertRowIndexToModel(linha);
    }

    public void aoDuploClique(Runnable acao) {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && getSelectedRow() >= 0) {
                    acao.run();
                }
            }
        });
    }

    public void larguras(int... larguras) {
        for (int i = 0; i < larguras.length && i < getColumnCount(); i++) {
            if (larguras[i] > 0) {
                getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
            }
        }
    }
}
