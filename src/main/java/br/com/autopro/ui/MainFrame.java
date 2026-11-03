package br.com.autopro.ui;

import br.com.autopro.ui.componentes.Notificacao;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.ConexaoBD;
import br.com.autopro.util.Formatador;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Janela principal: menu lateral, área de módulos (CardLayout), central de notificações e barra de status. */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String PAINEL = "Painel";
    public static final String CLIENTES = "Clientes";
    public static final String VEICULOS = "Veículos";
    public static final String ORDENS = "Ordens de serviço";
    public static final String ESTOQUE = "Estoque de peças";
    public static final String PAGAMENTOS = "Pagamentos";
    public static final String AGENDA = "Agenda e revisões";
    public static final String RELATORIOS = "Relatórios";

    private final transient ContextoApp contexto;
    private final CardLayout cartoes = new CardLayout();
    private final JPanel conteudo = new JPanel(cartoes);
    private final Map<String, Atualizavel> modulos = new LinkedHashMap<>();
    private final Map<String, JButton> botoesMenu = new LinkedHashMap<>();
    private final List<String> notificacoes = new ArrayList<>();
    private final JButton botaoNotificacoes = UiUtil.botao("Notificações (0)", e -> mostrarNotificacoes());
    private String moduloAtual;

    public MainFrame(ContextoApp contexto) {
        super("AutoPro - Gestão de Oficina Mecânica");
        this.contexto = contexto;
        contexto.setJanela(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1360, 840);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);

        conteudo.setBackground(Tema.FUNDO);
        registrar(PAINEL, new DashboardPanel(contexto));
        registrar(CLIENTES, new ClientesPanel(contexto));
        registrar(VEICULOS, new VeiculosPanel(contexto));
        registrar(ORDENS, new OrdensPanel(contexto));
        registrar(ESTOQUE, new EstoquePanel(contexto));
        registrar(PAGAMENTOS, new PagamentosPanel(contexto));
        registrar(AGENDA, new AgendamentosPanel(contexto));
        registrar(RELATORIOS, new RelatoriosPanel(contexto));

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(Tema.FUNDO);
        centro.add(criarBarraSuperior(), BorderLayout.NORTH);
        centro.add(conteudo, BorderLayout.CENTER);
        centro.add(criarBarraStatus(), BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(criarMenuLateral(), BorderLayout.WEST);
        getContentPane().add(centro, BorderLayout.CENTER);
        navegar(PAINEL);
    }

    private void registrar(String nome, JComponent painel) {
        painel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        conteudo.add(painel, nome);
        modulos.put(nome, (Atualizavel) painel);
    }

    private JComponent criarMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Tema.LATERAL);
        menu.setPreferredSize(new Dimension(230, 0));
        menu.setBorder(BorderFactory.createEmptyBorder(22, 0, 16, 0));

        JLabel logo = new JLabel("<html><span style='color:#E86A17'>Auto</span>"
                + "<span style='color:#FFFFFF'>Pro</span></html>");
        logo.setFont(Tema.fonte(Font.BOLD, 30));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        JLabel slogan = new JLabel("Gestão de Oficina Mecânica");
        slogan.setFont(Tema.fonte(Font.PLAIN, 12));
        slogan.setForeground(Tema.LATERAL_TEXTO);
        slogan.setAlignmentX(LEFT_ALIGNMENT);
        slogan.setBorder(BorderFactory.createEmptyBorder(0, 24, 26, 0));
        menu.add(logo);
        menu.add(slogan);

        for (String nome : modulos.keySet()) {
            JButton b = new JButton(nome);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(Tema.fonte(Font.PLAIN, 14));
            b.setForeground(Tema.LATERAL_TEXTO);
            b.setBackground(Tema.LATERAL);
            b.setBorder(BorderFactory.createEmptyBorder(11, 24, 11, 12));
            b.setFocusPainted(false);
            b.setContentAreaFilled(true);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            b.setAlignmentX(LEFT_ALIGNMENT);
            b.putClientProperty("JButton.buttonType", "borderless");
            b.addActionListener(e -> navegar(nome));
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!nome.equals(moduloAtual)) {
                        b.setBackground(Tema.LATERAL_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!nome.equals(moduloAtual)) {
                        b.setBackground(Tema.LATERAL);
                    }
                }
            });
            botoesMenu.put(nome, b);
            menu.add(b);
        }
        menu.add(Box.createVerticalGlue());
        JLabel versao = new JLabel("v1.0 - Projeto acadêmico");
        versao.setFont(Tema.fonte(Font.PLAIN, 11));
        versao.setForeground(Tema.TEXTO_SECUNDARIO);
        versao.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
        versao.setAlignmentX(LEFT_ALIGNMENT);
        menu.add(versao);
        return menu;
    }

    private JComponent criarBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Color.WHITE);
        barra.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(8, 20, 8, 16)));
        JLabel data = new JLabel(Formatador.data(LocalDate.now()));
        data.setForeground(Tema.TEXTO_SECUNDARIO);
        barra.add(data, BorderLayout.WEST);
        barra.add(UiUtil.linhaBotoes(FlowLayout.RIGHT, botaoNotificacoes), BorderLayout.EAST);
        return barra;
    }

    private JComponent criarBarraStatus() {
        JLabel status = new JLabel("Conectado a " + ConexaoBD.descricao());
        status.setFont(Tema.fonte(Font.PLAIN, 11));
        status.setForeground(Tema.TEXTO_SECUNDARIO);
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(4, 20, 4, 20)));
        status.setOpaque(true);
        status.setBackground(Color.WHITE);
        return status;
    }

    /** Exibe o módulo e recarrega seus dados do banco. */
    public void navegar(String nome) {
        Atualizavel modulo = modulos.get(nome);
        if (modulo == null) {
            return;
        }
        moduloAtual = nome;
        botoesMenu.forEach((n, b) -> {
            boolean ativo = n.equals(nome);
            b.setBackground(ativo ? Tema.PRIMARIA : Tema.LATERAL);
            b.setForeground(ativo ? Color.WHITE : Tema.LATERAL_TEXTO);
            b.setFont(Tema.fonte(ativo ? Font.BOLD : Font.PLAIN, 14));
        });
        cartoes.show(conteudo, nome);
        modulo.atualizar();
    }

    /** Registra uma notificação na central e exibe um aviso temporário na tela. */
    public void notificar(String titulo, String mensagem, Color cor) {
        notificacoes.add(0, Formatador.dataHora(LocalDateTime.now()) + " - " + titulo + ": " + mensagem);
        botaoNotificacoes.setText("Notificações (" + notificacoes.size() + ")");
        botaoNotificacoes.setForeground(Tema.PRIMARIA);
        Notificacao.exibir(this, titulo, mensagem, cor);
    }

    private void mostrarNotificacoes() {
        botaoNotificacoes.setForeground(Tema.TEXTO);
        if (notificacoes.isEmpty()) {
            UiUtil.sucesso(this, "Nenhuma notificação nesta sessão.");
            return;
        }
        JList<String> lista = new JList<>(notificacoes.toArray(new String[0]));
        JScrollPane sp = new JScrollPane(lista);
        sp.setPreferredSize(new Dimension(620, 260));
        javax.swing.JOptionPane.showMessageDialog(this, sp, "Notificações da sessão",
                javax.swing.JOptionPane.PLAIN_MESSAGE);
    }
}
