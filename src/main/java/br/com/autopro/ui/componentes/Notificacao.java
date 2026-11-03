package br.com.autopro.ui.componentes;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Notificação temporária ("toast") exibida no canto inferior direito da janela principal. */
public final class Notificacao {

    private static final int DURACAO_MS = 7000;

    private Notificacao() {
    }

    public static void exibir(Window dona, String titulo, String mensagem, Color cor) {
        JWindow janela = new JWindow(dona);
        JPanel painel = new JPanel(new BorderLayout(0, 4));
        painel.setBackground(Color.WHITE);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 6, 1, 1, cor),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.fonte(Font.BOLD, 14));
        t.setForeground(cor);
        JLabel m = new JLabel("<html><div style='width:280px'>" + mensagem + "</div></html>");
        m.setFont(Tema.fonte(Font.PLAIN, 13));
        painel.add(t, BorderLayout.NORTH);
        painel.add(m, BorderLayout.CENTER);
        janela.setContentPane(painel);
        janela.pack();
        Point p = dona.getLocationOnScreen();
        janela.setLocation(p.x + dona.getWidth() - janela.getWidth() - 24,
                p.y + dona.getHeight() - janela.getHeight() - 48);
        janela.setAlwaysOnTop(true);
        painel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                janela.dispose();
            }
        });
        janela.setVisible(true);
        Timer timer = new Timer(DURACAO_MS, e -> janela.dispose());
        timer.setRepeats(false);
        timer.start();
    }
}
