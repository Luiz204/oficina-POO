package br.com.autopro.ui;

import br.com.autopro.model.HistoricoStatus;
import br.com.autopro.model.MovimentacaoEstoque;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.ui.componentes.DialogoConsulta;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;

public class OrdensPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;
    private static final String TODOS = "Todos os status";

    private final transient ContextoApp ctx;
    private final JComboBox<Object> filtroStatus = new JComboBox<>();
    private final JTextField busca = UiUtil.campoBusca("Nº da OS, placa, modelo ou cliente", this::atualizar);
    private final Tabela<OrdemServico> tabela = FabricaTabelas.ordens();
    private final JLabel resumo = new JLabel(" ");

    private final JButton btIniciar = UiUtil.botao("Iniciar serviço", e -> iniciar());
    private final JButton btPronta = UiUtil.botao("Marcar como pronta", e -> marcarPronta());
    private final JButton btPagar = UiUtil.botao("Registrar pagamento", e -> pagar());
    private final JButton btEntregar = UiUtil.botaoSucesso("Entregar veículo", e -> entregar());
    private final JButton btVerificar = UiUtil.botao("Verificar peças", e -> verificarPecas());
    private final JButton btExcluir = UiUtil.botaoPerigo("Excluir", e -> excluir());

    public OrdensPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        filtroStatus.addItem(TODOS);
        for (StatusOrdem s : StatusOrdem.values()) {
            filtroStatus.addItem(s);
        }
        filtroStatus.addActionListener(e -> atualizar());
        tabela.aoDuploClique(this::abrir);
        tabela.aoSelecionar(this::atualizarBotoes);
        resumo.setFont(Tema.fonte(Font.PLAIN, 13));

        add(UiUtil.cabecalho("Ordens de serviço", "Mão de obra: R$ 120,00/h - VIP: 10% de desconto na mão de obra. "
                + "Fluxo: Aguardando peças → Em serviço → Pronto para entrega → Entregue."), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(UiUtil.linhaBotoes(FlowLayout.LEFT, filtroStatus, busca, UiUtil.botao("Pesquisar", e -> atualizar())),
                BorderLayout.NORTH);
        centro.add(tabela.comRolagem(), BorderLayout.CENTER);
        centro.add(resumo, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);

        JPanel acoes = new JPanel(new BorderLayout());
        acoes.setOpaque(false);
        acoes.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Nova OS", e -> nova()),
                UiUtil.botao("Abrir / editar", e -> abrir()),
                btVerificar, btIniciar, btPronta, btPagar, btEntregar), BorderLayout.NORTH);
        acoes.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botao("Histórico da ordem", e -> historico()), btExcluir), BorderLayout.SOUTH);
        add(acoes, BorderLayout.SOUTH);
        atualizarBotoes();
    }

    @Override
    public void atualizar() {
        Object f = filtroStatus.getSelectedItem();
        StatusOrdem status = f instanceof StatusOrdem ? (StatusOrdem) f : null;
        UiUtil.executar(this, () -> tabela.setDados(ctx.ordens().listar(status, busca.getText())));
        atualizarBotoes();
    }

    private void atualizarBotoes() {
        OrdemServico o = tabela.getSelecionado();
        StatusOrdem s = o == null ? null : o.getStatus();
        btVerificar.setEnabled(s == StatusOrdem.AGUARDANDO_PECAS);
        btIniciar.setEnabled(s == StatusOrdem.AGUARDANDO_PECAS);
        btPronta.setEnabled(s == StatusOrdem.EM_SERVICO);
        btPagar.setEnabled(s == StatusOrdem.PRONTO_PARA_ENTREGA && !o.isPaga());
        btEntregar.setEnabled(s == StatusOrdem.PRONTO_PARA_ENTREGA);
        btExcluir.setEnabled(s == StatusOrdem.AGUARDANDO_PECAS);
        if (o == null) {
            resumo.setText(tabela.getDados().size() + " ordem(ns). Selecione uma ordem para ver as ações disponíveis.");
        } else {
            resumo.setText(String.format("<html><b>OS #%d</b> - Peças: %s | Mão de obra (%s h): %s | Desconto VIP: %s"
                            + " | <b>Total: %s</b> | Pagamento: %s</html>", o.getId(),
                    Formatador.moeda(o.getValorPecas()), Formatador.decimal(o.getHorasMaoObra()),
                    Formatador.moeda(o.getValorMaoObra()), Formatador.moeda(o.getDesconto()),
                    Formatador.moeda(o.getValorTotal()), o.getSituacaoPagamento()));
        }
    }

    private Optional<OrdemServico> selecionada() {
        return UiUtil.exigirSelecao(this, tabela.getSelecionado(), "uma ordem de serviço");
    }

    private void nova() {
        if (new OrdemDialog(this, ctx, null, null, null, null).exibir()) {
            atualizar();
        }
    }

    private void abrir() {
        selecionada().ifPresent(o -> UiUtil.obter(this, () -> ctx.ordens().buscar(o.getId())).ifPresent(completa -> {
            if (new OrdemDialog(this, ctx, completa, null, null, null).exibir()) {
                atualizar();
            }
        }));
    }

    private void verificarPecas() {
        selecionada().ifPresent(o -> UiUtil.obter(this, () -> ctx.ordens().verificarDisponibilidade(o.getId()))
                .ifPresent(pendencias -> {
                    if (pendencias.isEmpty()) {
                        UiUtil.sucesso(this, "Todas as peças da OS #" + o.getId()
                                + " estão disponíveis. O serviço pode ser iniciado.");
                    } else {
                        UiUtil.aviso(this, "Peças em falta para a OS #" + o.getId() + ":\n• "
                                + String.join("\n• ", pendencias));
                    }
                }));
    }

    private void iniciar() {
        selecionada().ifPresent(o -> {
            if (UiUtil.confirmar(this, "Iniciar o serviço da OS #" + o.getId() + " (" + o.getPlaca() + ")?\n"
                    + "As peças necessárias serão baixadas do estoque.")
                    && UiUtil.executar(this, () -> ctx.ordens().iniciarServico(o.getId()))) {
                UiUtil.sucesso(this, "Serviço iniciado. O estoque foi atualizado.");
                atualizar();
            }
        });
    }

    private void marcarPronta() {
        selecionada().ifPresent(o -> {
            if (!UiUtil.confirmar(this, "Concluir o serviço da OS #" + o.getId() + " e marcar o veículo "
                    + o.getPlaca() + " como pronto para entrega?")) {
                return;
            }
            UiUtil.obter(this, () -> ctx.ordens().marcarPronta(o.getId())).ifPresent(pronta -> {
                ctx.notificar("Veículo pronto para entrega",
                        "OS #" + pronta.getId() + " - " + pronta.getPlaca() + " (" + pronta.getModeloVeiculo()
                                + ") de " + pronta.getClienteNome() + ". Valor a receber: "
                                + Formatador.moeda(pronta.getValorTotal()) + ".", Tema.SUCESSO);
                atualizar();
            });
        });
    }

    private void pagar() {
        selecionada().ifPresent(o -> {
            if (new PagamentoDialog(this, ctx, o).exibir()) {
                atualizar();
            }
        });
    }

    private void entregar() {
        selecionada().ifPresent(o -> {
            if (!o.isPaga()) {
                UiUtil.aviso(this, "A entrega está bloqueada: registre o pagamento da OS #" + o.getId()
                        + " (" + Formatador.moeda(o.getValorTotal()) + ") antes de entregar o veículo.");
                return;
            }
            if (UiUtil.confirmar(this, "Confirmar a entrega do veículo " + o.getPlaca() + " a " + o.getClienteNome()
                    + "?") && UiUtil.executar(this, () -> ctx.ordens().entregar(o.getId()))) {
                UiUtil.sucesso(this, "Veículo entregue. OS #" + o.getId() + " finalizada.");
                atualizar();
            }
        });
    }

    private void excluir() {
        selecionada().ifPresent(o -> {
            if (UiUtil.confirmar(this, "Excluir a OS #" + o.getId() + "?\nSomente ordens que não movimentaram estoque "
                    + "podem ser excluídas.") && UiUtil.executar(this, () -> ctx.ordens().excluir(o.getId()))) {
                UiUtil.sucesso(this, "Ordem excluída.");
                atualizar();
            }
        });
    }

    private void historico() {
        selecionada().ifPresent(o -> mostrarHistorico(this, ctx, o.getId()));
    }

    static void mostrarHistorico(java.awt.Component pai, ContextoApp ctx, int ordemId) {
        UiUtil.executar(pai, () -> {
            OrdemServico o = ctx.ordens().buscar(ordemId);
            List<HistoricoStatus> historico = ctx.ordens().historico(ordemId);
            List<MovimentacaoEstoque> movs = ctx.estoque().movimentacoesDaOrdem(ordemId);
            Optional<Pagamento> pagamento = ctx.pagamentos().buscarPorOrdem(ordemId);

            Tabela<HistoricoStatus> tHist = FabricaTabelas.historicoStatus();
            tHist.setDados(historico);
            Tabela<br.com.autopro.model.ItemOrdem> tItens = FabricaTabelas.itens();
            tItens.setDados(o.getItens());
            Tabela<MovimentacaoEstoque> tMovs = FabricaTabelas.movimentacoes();
            tMovs.setDados(movs);
            Tabela<Pagamento> tPag = FabricaTabelas.pagamentos();
            tPag.setDados(pagamento.map(List::of).orElse(List.of()));

            new DialogoConsulta(pai, "OS #" + o.getId() + " - " + o.getStatus().getDescricao(),
                    o.getPlaca() + " " + o.getModeloVeiculo() + " - " + o.getClienteNome() + " - aberta em "
                            + Formatador.dataHora(o.getDataAbertura()) + " - total " + Formatador.moeda(o.getValorTotal()))
                    .aba("Histórico de status", tHist.comRolagem())
                    .aba("Peças da ordem", tItens.comRolagem())
                    .aba("Movimentações de estoque", tMovs.comRolagem())
                    .aba("Pagamento", tPag.comRolagem())
                    .exibir();
        });
    }
}
