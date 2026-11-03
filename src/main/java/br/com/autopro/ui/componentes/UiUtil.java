package br.com.autopro.ui.componentes;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.exception.PersistenciaException;
import br.com.autopro.util.Log;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fábrica de componentes padronizados e tratamento uniforme de mensagens/erros. */
public final class UiUtil {

    private static final Logger LOG = Log.get(UiUtil.class);

    private UiUtil() {
    }

    // --------------------------------------------------------- botões

    public static JButton botao(String texto, ActionListener acao) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(Tema.fonte(Font.PLAIN, 13));
        if (acao != null) {
            b.addActionListener(acao);
        }
        return b;
    }

    public static JButton botaoPrimario(String texto, ActionListener acao) {
        return colorido(botao(texto, acao), Tema.PRIMARIA);
    }

    public static JButton botaoPerigo(String texto, ActionListener acao) {
        return colorido(botao(texto, acao), Tema.PERIGO);
    }

    public static JButton botaoSucesso(String texto, ActionListener acao) {
        return colorido(botao(texto, acao), Tema.SUCESSO);
    }

    private static JButton colorido(JButton b, Color cor) {
        b.setBackground(cor);
        b.setForeground(Color.WHITE);
        b.setFont(Tema.fonte(Font.BOLD, 13));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    // --------------------------------------------------------- rótulos e painéis

    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(Font.BOLD, 22));
        l.setForeground(Tema.TEXTO);
        return l;
    }

    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(Font.PLAIN, 13));
        l.setForeground(Tema.TEXTO_SECUNDARIO);
        return l;
    }

    public static JLabel secao(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.fonte(Font.BOLD, 15));
        l.setForeground(Tema.TEXTO);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));
        return l;
    }

    public static JPanel linhaBotoes(int alinhamento, JComponent... componentes) {
        JPanel p = new JPanel(new FlowLayout(alinhamento, 8, 4));
        p.setOpaque(false);
        for (JComponent c : componentes) {
            p.add(c);
        }
        return p;
    }

    /** Cabeçalho padrão dos módulos: título, subtítulo e componentes à direita. */
    public static JPanel cabecalho(String titulo, String subtitulo, JComponent... direita) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel textos = new JPanel(new BorderLayout());
        textos.setOpaque(false);
        textos.add(titulo(titulo), BorderLayout.NORTH);
        textos.add(subtitulo(subtitulo), BorderLayout.CENTER);
        p.add(textos, BorderLayout.CENTER);
        p.add(linhaBotoes(FlowLayout.RIGHT, direita), BorderLayout.EAST);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return p;
    }

    public static JTextField campoBusca(String dica, Runnable aoPesquisar) {
        JTextField campo = new JTextField(24);
        campo.putClientProperty("JTextField.placeholderText", dica);
        campo.putClientProperty("JTextField.showClearButton", true);
        campo.addActionListener(e -> aoPesquisar.run());
        return campo;
    }

    /** Painel branco com borda, usado como "cartão" para agrupar conteúdo. */
    public static JPanel cartao(Component conteudo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Tema.CARTAO);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BORDA),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    // --------------------------------------------------------- mensagens

    public static void sucesso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void aviso(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    public static void erro(Component pai, String mensagem) {
        JOptionPane.showMessageDialog(pai, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component pai, String mensagem) {
        return JOptionPane.showConfirmDialog(pai, mensagem, "Confirmação", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Pede que o usuário selecione um registro antes de executar a ação. */
    public static <T> Optional<T> exigirSelecao(Component pai, T selecionado, String oQue) {
        if (selecionado == null) {
            aviso(pai, "Selecione " + oQue + " na tabela.");
            return Optional.empty();
        }
        return Optional.of(selecionado);
    }

    // --------------------------------------------------------- execução com tratamento de erros

    /**
     * Executa a ação exibindo mensagens adequadas: regras de negócio como alerta,
     * falhas de banco como erro. Retorna true se a ação terminou sem erros.
     */
    public static boolean executar(Component pai, Runnable acao) {
        return obter(pai, () -> {
            acao.run();
            return Boolean.TRUE;
        }).isPresent();
    }

    public static <T> Optional<T> obter(Component pai, Supplier<T> acao) {
        Cursor anterior = pai != null ? pai.getCursor() : null;
        try {
            if (pai != null) {
                pai.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            }
            return Optional.ofNullable(acao.get());
        } catch (NegocioException e) {
            aviso(pai, e.getMessage());
        } catch (PersistenciaException e) {
            erro(pai, e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Erro inesperado", e);
            erro(pai, "Erro inesperado: " + e.getClass().getSimpleName()
                    + ". Detalhes registrados em logs/autopro.log.");
        } finally {
            if (pai != null) {
                pai.setCursor(anterior);
            }
        }
        return Optional.empty();
    }
}
