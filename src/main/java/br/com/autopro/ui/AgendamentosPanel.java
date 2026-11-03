package br.com.autopro.ui;

import br.com.autopro.model.Agendamento;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.StatusAgendamento;
import br.com.autopro.model.TipoAgendamento;
import br.com.autopro.model.Veiculo;
import br.com.autopro.service.RegraRevisao;
import br.com.autopro.ui.componentes.ModeloTabela;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Agendamentos, revisões periódicas e atendimento prioritário VIP. */
public class AgendamentosPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;
    private static final String PROXIMOS = "Próximos agendamentos";
    private static final String TODOS = "Todos";

    private final transient ContextoApp ctx;
    private final JTabbedPane abas = new JTabbedPane();

    // Aba agendamentos
    private final JComboBox<Object> filtroAgenda = new JComboBox<>();
    private final Tabela<Agendamento> tabelaAgenda = FabricaTabelas.agendamentos();

    // Aba revisões
    private final JCheckBox todosVeiculos = new JCheckBox("Mostrar todos os veículos");
    private final Tabela<Veiculo> tabelaRevisoes;

    // Aba VIP
    private final JTextField duracaoRapida = new JTextField(6);
    private final JLabel situacaoDuracao = new JLabel();
    private final Tabela<OrdemServico> tabelaFila = FabricaTabelas.ordens();
    private final Tabela<OrdemServico> tabelaElegiveis = FabricaTabelas.ordens();
    private final JLabel avisoElegiveis = new JLabel(" ");

    public AgendamentosPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        add(UiUtil.cabecalho("Agenda e revisões", "Revisão a cada 10.000 km ou 6 meses (o que ocorrer primeiro). "
                + "Clientes VIP (em amarelo) têm prioridade."), BorderLayout.NORTH);

        ModeloTabela<Veiculo> mRev = new ModeloTabela<Veiculo>()
                .coluna("Placa", Veiculo::getPlaca)
                .coluna("Modelo", Veiculo::getModelo)
                .coluna("Proprietário", v -> FabricaTabelas.nomeComVip(v.getClienteNome(), v.isProprietarioVip()))
                .coluna("Km atual", Integer.class, Veiculo::getKmAtual)
                .coluna("Última revisão", LocalDate.class, Veiculo::getDataUltimaRevisao)
                .coluna("Km últ. revisão", Integer.class, Veiculo::getKmUltimaRevisao)
                .coluna("Revisar até (data)", LocalDate.class, RegraRevisao::proximaPorData)
                .coluna("Revisar até (km)", Integer.class, RegraRevisao::proximaPorKm)
                .coluna("Situação", v -> ctx.veiculos().situacaoRevisao(v));
        tabelaRevisoes = new Tabela<>(mRev)
                .destacar(v -> ctx.veiculos().situacaoRevisao(v).requerAtencao(), Tema.DESTAQUE_ALERTA)
                .larguras(80, 170, 190, 85, 100, 100, 115, 110, 170);

        abas.addTab("Agendamentos", criarAbaAgenda());
        abas.addTab("Revisões", criarAbaRevisoes());
        abas.addTab("Atendimento VIP", criarAbaVip());
        abas.addChangeListener(e -> atualizar());
        add(abas, BorderLayout.CENTER);
    }

    private JPanel criarAbaAgenda() {
        filtroAgenda.addItem(PROXIMOS);
        filtroAgenda.addItem(TODOS);
        for (StatusAgendamento s : StatusAgendamento.values()) {
            filtroAgenda.addItem(s);
        }
        filtroAgenda.addActionListener(e -> atualizar());
        tabelaAgenda.aoDuploClique(this::editarAgendamento);
        JPanel p = painelAba();
        p.add(UiUtil.linhaBotoes(FlowLayout.LEFT, new JLabel("Exibir:"), filtroAgenda), BorderLayout.NORTH);
        p.add(tabelaAgenda.comRolagem(), BorderLayout.CENTER);
        p.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Novo agendamento", e -> novoAgendamento(null, null)),
                UiUtil.botao("Editar", e -> editarAgendamento()),
                UiUtil.botao("Abrir OS do agendamento", e -> abrirOs()),
                UiUtil.botaoSucesso("Marcar como concluído", e -> concluir()),
                UiUtil.botaoPerigo("Cancelar agendamento", e -> cancelar())), BorderLayout.SOUTH);
        return p;
    }

    private JPanel criarAbaRevisoes() {
        todosVeiculos.setOpaque(false);
        todosVeiculos.addActionListener(e -> atualizar());
        JPanel p = painelAba();
        p.add(UiUtil.linhaBotoes(FlowLayout.LEFT, new JLabel("Veículos com revisão vencida ou sem histórico "
                + "(devem ser avaliados)."), todosVeiculos), BorderLayout.NORTH);
        p.add(tabelaRevisoes.comRolagem(), BorderLayout.CENTER);
        p.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Registrar revisão", e -> registrarRevisao()),
                UiUtil.botao("Agendar revisão preventiva", e -> agendarRevisao()),
                UiUtil.botao("Histórico do veículo", e -> UiUtil.exigirSelecao(this, tabelaRevisoes.getSelecionado(),
                        "um veículo").ifPresent(v -> VeiculosPanel.mostrarHistorico(this, ctx, v)))), BorderLayout.SOUTH);
        return p;
    }

    private JPanel criarAbaVip() {
        situacaoDuracao.setFont(Tema.fonte(Font.BOLD, 13));
        avisoElegiveis.setForeground(Tema.TEXTO_SECUNDARIO);
        JPanel config = UiUtil.linhaBotoes(FlowLayout.LEFT,
                new JLabel("Duração máxima do atendimento/revisão rápida (minutos):"), duracaoRapida,
                UiUtil.botaoPrimario("Salvar parâmetro", e -> salvarDuracao()), situacaoDuracao);

        JPanel fila = new JPanel(new BorderLayout(0, 4));
        fila.setOpaque(false);
        fila.add(UiUtil.secao("Fila de atendimento - ordens abertas, VIP primeiro"), BorderLayout.NORTH);
        fila.add(tabelaFila.comRolagem(), BorderLayout.CENTER);

        JPanel elegiveis = new JPanel(new BorderLayout(0, 4));
        elegiveis.setOpaque(false);
        elegiveis.add(UiUtil.secao("Ordens VIP elegíveis para atendimento rápido"), BorderLayout.NORTH);
        elegiveis.add(tabelaElegiveis.comRolagem(), BorderLayout.CENTER);
        elegiveis.add(avisoElegiveis, BorderLayout.SOUTH);

        JPanel listas = new JPanel(new GridLayout(2, 1, 0, 8));
        listas.setOpaque(false);
        listas.add(fila);
        listas.add(elegiveis);

        JPanel p = painelAba();
        p.add(config, BorderLayout.NORTH);
        p.add(listas, BorderLayout.CENTER);
        p.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botao("Atualizar", e -> atualizar()),
                UiUtil.botao("Histórico da ordem", e -> {
                    OrdemServico o = Optional.ofNullable(tabelaElegiveis.getSelecionado())
                            .orElse(tabelaFila.getSelecionado());
                    UiUtil.exigirSelecao(this, o, "uma ordem").ifPresent(x ->
                            OrdensPanel.mostrarHistorico(this, ctx, x.getId()));
                })), BorderLayout.SOUTH);
        return p;
    }

    private JPanel painelAba() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return p;
    }

    @Override
    public void atualizar() {
        int aba = abas.getSelectedIndex();
        UiUtil.executar(this, () -> {
            if (aba == 0) {
                Object f = filtroAgenda.getSelectedItem();
                List<Agendamento> lista;
                if (PROXIMOS.equals(f)) {
                    lista = ctx.agendamentos().listarFuturos();
                } else if (f instanceof StatusAgendamento) {
                    lista = ctx.agendamentos().listar((StatusAgendamento) f);
                } else {
                    lista = ctx.agendamentos().listar(null);
                }
                tabelaAgenda.setDados(lista);
            } else if (aba == 1) {
                tabelaRevisoes.setDados(todosVeiculos.isSelected() ? ctx.veiculos().listarTodos()
                        : ctx.veiculos().listarComRevisaoPendente(""));
            } else {
                atualizarVip();
            }
        });
    }

    private void atualizarVip() {
        Optional<Integer> duracao = ctx.configuracao().duracaoRevisaoRapida();
        duracaoRapida.setText(duracao.map(String::valueOf).orElse(""));
        situacaoDuracao.setText(duracao.isPresent() ? "Configurado: " + duracao.get() + " min"
                : "Parâmetro não configurado");
        situacaoDuracao.setForeground(duracao.isPresent() ? Tema.SUCESSO : Tema.ALERTA);
        tabelaFila.setDados(ctx.ordens().filaAtendimento());
        if (duracao.isPresent()) {
            List<OrdemServico> elegiveis = ctx.ordens().elegiveisAtendimentoRapido();
            tabelaElegiveis.setDados(elegiveis);
            avisoElegiveis.setText("Critérios: cliente VIP, aguardando início, todas as peças em estoque e mão de obra "
                    + "até " + duracao.get() + " min. " + elegiveis.size() + " ordem(ns) elegível(is).");
        } else {
            tabelaElegiveis.setDados(List.of());
            avisoElegiveis.setText("Defina a duração do atendimento rápido para identificar as ordens elegíveis.");
        }
    }

    private void salvarDuracao() {
        String texto = duracaoRapida.getText();
        if (UiUtil.executar(this, () -> ctx.configuracao().definirDuracaoRevisaoRapida(
                texto == null || texto.isBlank() ? null : Formatador.parseInteiro(texto, "Duração")))) {
            UiUtil.sucesso(this, texto == null || texto.isBlank() ? "Parâmetro removido." : "Parâmetro salvo.");
            atualizar();
        }
    }

    private void novoAgendamento(Integer veiculoId, TipoAgendamento tipo) {
        if (new AgendamentoDialog(this, ctx, null, veiculoId, tipo).exibir()) {
            atualizar();
        }
    }

    private void editarAgendamento() {
        UiUtil.exigirSelecao(this, tabelaAgenda.getSelecionado(), "um agendamento").ifPresent(a -> {
            if (a.getStatus() != StatusAgendamento.AGENDADO) {
                UiUtil.aviso(this, "Somente agendamentos com status \"Agendado\" podem ser alterados.");
                return;
            }
            if (new AgendamentoDialog(this, ctx, a, null, null).exibir()) {
                atualizar();
            }
        });
    }

    private void abrirOs() {
        UiUtil.exigirSelecao(this, tabelaAgenda.getSelecionado(), "um agendamento").ifPresent(a -> {
            if (a.getOrdemId() != null) {
                OrdensPanel.mostrarHistorico(this, ctx, a.getOrdemId());
                return;
            }
            if (a.getStatus() != StatusAgendamento.AGENDADO) {
                UiUtil.aviso(this, "O agendamento não está ativo.");
                return;
            }
            String descricao = a.getTipo().getDescricao() + (a.getDescricao() == null ? "" : ": " + a.getDescricao());
            OrdemDialog dialogo = new OrdemDialog(this, ctx, null, a.getVeiculoId(),
                    a.getDataHora().toLocalDate(), descricao);
            if (dialogo.exibir() && dialogo.getOrdemSalva() != null) {
                UiUtil.executar(this, () -> ctx.agendamentos().vincularOrdem(a.getId(), dialogo.getOrdemSalva().getId()));
                atualizar();
            }
        });
    }

    private void concluir() {
        UiUtil.exigirSelecao(this, tabelaAgenda.getSelecionado(), "um agendamento").ifPresent(a -> {
            if (UiUtil.confirmar(this, "Marcar o agendamento de " + a.getPlaca() + " em "
                    + Formatador.dataHora(a.getDataHora()) + " como concluído?")
                    && UiUtil.executar(this, () -> ctx.agendamentos().concluir(a.getId()))) {
                atualizar();
            }
        });
    }

    private void cancelar() {
        UiUtil.exigirSelecao(this, tabelaAgenda.getSelecionado(), "um agendamento").ifPresent(a -> {
            if (UiUtil.confirmar(this, "Cancelar o agendamento de " + a.getPlaca() + " em "
                    + Formatador.dataHora(a.getDataHora()) + "?")
                    && UiUtil.executar(this, () -> ctx.agendamentos().cancelar(a.getId()))) {
                atualizar();
            }
        });
    }

    private void registrarRevisao() {
        UiUtil.exigirSelecao(this, tabelaRevisoes.getSelecionado(), "um veículo").ifPresent(v -> {
            if (new RevisaoDialog(this, ctx, v).exibir()) {
                atualizar();
            }
        });
    }

    private void agendarRevisao() {
        UiUtil.exigirSelecao(this, tabelaRevisoes.getSelecionado(), "um veículo")
                .ifPresent(v -> novoAgendamento(v.getId(), TipoAgendamento.REVISAO_PREVENTIVA));
    }
}
