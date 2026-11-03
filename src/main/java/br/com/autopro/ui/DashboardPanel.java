package br.com.autopro.ui;

import br.com.autopro.model.Indicadores;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Agendamento;
import br.com.autopro.service.DashboardService;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/** Painel inicial com indicadores obtidos do banco de dados. */
public class DashboardPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final List<Cartao> cartoes = new ArrayList<>();
    private final JLabel atualizadoEm = UiUtil.subtitulo("");
    private final Tabela<Agendamento> tabelaAgenda = FabricaTabelas.agendamentos();
    private final Tabela<OrdemServico> tabelaFila = FabricaTabelas.ordens();

    /** Cartão clicável com um indicador numérico. */
    private final class Cartao extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JLabel valor = new JLabel("-");
        private final transient ToIntFunction<Indicadores> extrator;

        Cartao(String titulo, Color cor, String destino, ToIntFunction<Indicadores> extrator) {
            super(new BorderLayout(0, 2));
            this.extrator = extrator;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, cor),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            valor.setFont(Tema.fonte(Font.BOLD, 30));
            valor.setForeground(cor);
            JLabel rotulo = new JLabel(titulo);
            rotulo.setFont(Tema.fonte(Font.PLAIN, 13));
            rotulo.setForeground(Tema.TEXTO_SECUNDARIO);
            add(valor, BorderLayout.CENTER);
            add(rotulo, BorderLayout.SOUTH);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Abrir " + destino);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ctx.navegar(destino);
                }
            });
        }

        void exibir(Indicadores i) {
            valor.setText(Formatador.inteiro(extrator.applyAsInt(i)));
        }
    }

    public DashboardPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 12));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        add(UiUtil.cabecalho("Painel", "Visão geral da oficina", atualizadoEm,
                UiUtil.botao("Atualizar", e -> atualizar())), BorderLayout.NORTH);

        JPanel grade = new JPanel(new GridLayout(3, 3, 12, 12));
        grade.setOpaque(false);
        cartoes.add(new Cartao("Clientes cadastrados", Tema.INFO, MainFrame.CLIENTES, Indicadores::getTotalClientes));
        cartoes.add(new Cartao("Veículos cadastrados", Tema.INFO, MainFrame.VEICULOS, Indicadores::getTotalVeiculos));
        cartoes.add(new Cartao("Ordens aguardando peças", Tema.ALERTA, MainFrame.ORDENS,
                Indicadores::getOrdensAguardandoPecas));
        cartoes.add(new Cartao("Ordens em serviço", Tema.INFO, MainFrame.ORDENS, Indicadores::getOrdensEmServico));
        cartoes.add(new Cartao("Veículos prontos para entrega", Tema.SUCESSO, MainFrame.ORDENS,
                Indicadores::getVeiculosProntos));
        cartoes.add(new Cartao("Ordens aguardando pagamento", Tema.ROXO, MainFrame.PAGAMENTOS,
                Indicadores::getOrdensAguardandoPagamento));
        cartoes.add(new Cartao("Veículos com revisão vencida/sem histórico", Tema.PERIGO, MainFrame.AGENDA,
                Indicadores::getRevisoesVencidas));
        cartoes.add(new Cartao("Peças abaixo do estoque mínimo", Tema.PERIGO, MainFrame.ESTOQUE,
                Indicadores::getPecasAbaixoMinimo));
        cartoes.add(new Cartao("Agendamentos nos próximos " + DashboardService.DIAS_PROXIMOS_AGENDAMENTOS + " dias",
                Tema.PRIMARIA, MainFrame.AGENDA, Indicadores::getProximosAgendamentos));
        cartoes.forEach(grade::add);

        JPanel listas = new JPanel(new GridLayout(1, 2, 12, 0));
        listas.setOpaque(false);
        listas.add(bloco("Próximos agendamentos", tabelaAgenda.comRolagem()));
        listas.add(bloco("Fila de atendimento (VIP primeiro)", tabelaFila.comRolagem()));

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(grade, BorderLayout.NORTH);
        centro.add(listas, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    private JPanel bloco(String titulo, java.awt.Component conteudo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(UiUtil.secao(titulo), BorderLayout.NORTH);
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void atualizar() {
        UiUtil.executar(this, () -> {
            Indicadores i = ctx.dashboard().carregar();
            cartoes.forEach(c -> c.exibir(i));
            LocalDateTime limite = LocalDateTime.now().plusDays(DashboardService.DIAS_PROXIMOS_AGENDAMENTOS);
            tabelaAgenda.setDados(ctx.agendamentos().listarFuturos().stream()
                    .filter(a -> a.getDataHora().isBefore(limite)).collect(Collectors.toList()));
            tabelaFila.setDados(ctx.ordens().filaAtendimento());
            atualizadoEm.setText("Atualizado em " + Formatador.dataHora(LocalDateTime.now()) + "   ");
        });
    }
}
