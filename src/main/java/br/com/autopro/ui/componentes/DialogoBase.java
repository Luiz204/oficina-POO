package br.com.autopro.ui.componentes;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;

/**
 * Diálogo modal padrão com área de conteúdo e botões Salvar/Cancelar.
 * Subclasses implementam {@link #salvar()} e retornam true quando a operação foi concluída.
 */
public abstract class DialogoBase extends JDialog {

    private static final long serialVersionUID = 1L;

    private boolean confirmado;
    protected final JButton botaoSalvar;
    protected final JButton botaoCancelar;

    protected DialogoBase(Component pai, String titulo) {
        super(pai instanceof Window ? (Window) pai : SwingUtilities.getWindowAncestor(pai), titulo,
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        botaoSalvar = UiUtil.botaoPrimario("Salvar", e -> aoSalvar());
        botaoCancelar = UiUtil.botao("Cancelar", e -> dispose());
        getRootPane().setDefaultButton(botaoSalvar);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Monta o diálogo com o conteúdo informado. Deve ser chamado no construtor da subclasse. */
    protected void montar(JComponent conteudo) {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        raiz.add(conteudo, BorderLayout.CENTER);
        raiz.add(UiUtil.linhaBotoes(FlowLayout.RIGHT, botaoCancelar, botaoSalvar), BorderLayout.SOUTH);
        setContentPane(raiz);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    private void aoSalvar() {
        if (UiUtil.obter(this, this::salvar).orElse(false)) {
            confirmado = true;
            dispose();
        }
    }

    /** Valida e grava os dados. Lança NegocioException em caso de erro de validação. */
    protected abstract boolean salvar();

    /** Exibe o diálogo e retorna true se o usuário salvou com sucesso. */
    public boolean exibir() {
        setVisible(true);
        return confirmado;
    }

}
