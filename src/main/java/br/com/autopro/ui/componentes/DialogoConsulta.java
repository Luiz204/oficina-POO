package br.com.autopro.ui.componentes;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Diálogo somente leitura com abas, usado para históricos e consultas. */
public class DialogoConsulta extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane abas = new JTabbedPane();
    private final JPanel topo = new JPanel(new BorderLayout());

    public DialogoConsulta(Component pai, String titulo, String descricao) {
        super(SwingUtilities.getWindowAncestor(pai), titulo, ModalityType.APPLICATION_MODAL);
        JPanel raiz = new JPanel(new BorderLayout(0, 8));
        raiz.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        topo.setOpaque(false);
        topo.add(UiUtil.titulo(titulo), BorderLayout.NORTH);
        if (descricao != null) {
            topo.add(UiUtil.subtitulo(descricao), BorderLayout.CENTER);
        }
        raiz.add(topo, BorderLayout.NORTH);
        raiz.add(abas, BorderLayout.CENTER);
        raiz.add(UiUtil.linhaBotoes(FlowLayout.RIGHT, UiUtil.botao("Fechar", e -> dispose())), BorderLayout.SOUTH);
        setContentPane(raiz);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public DialogoConsulta aba(String titulo, JComponent conteudo) {
        abas.addTab(titulo, conteudo);
        return this;
    }

    public void exibir() {
        setSize(new Dimension(920, 520));
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }
}
