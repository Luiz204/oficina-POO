package br.com.autopro.ui.componentes;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Painel de formulário em duas colunas (rótulo : campo) com GridBagLayout. */
public class Formulario extends JPanel {

    private static final long serialVersionUID = 1L;

    private int linha;

    public Formulario() {
        super(new GridBagLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public Formulario campo(String rotulo, JComponent campo) {
        return campo(rotulo, campo, false);
    }

    public Formulario campo(String rotulo, JComponent campo, boolean obrigatorio) {
        JLabel l = new JLabel(rotulo + (obrigatorio ? " *" : "") + ":");
        l.setFont(Tema.fonte(Font.PLAIN, 13));
        l.setLabelFor(campo);
        GridBagConstraints gl = new GridBagConstraints();
        gl.gridx = 0;
        gl.gridy = linha;
        gl.anchor = GridBagConstraints.NORTHEAST;
        gl.insets = new Insets(8, 4, 4, 10);
        add(l, gl);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 1;
        gc.gridy = linha;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 4);
        add(campo, gc);
        linha++;
        return this;
    }

    /** Linha que ocupa as duas colunas (ex.: dica ou aviso). */
    public Formulario linhaInteira(JComponent componente) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = linha++;
        g.gridwidth = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 4, 4, 4);
        add(componente, g);
        return this;
    }
}
