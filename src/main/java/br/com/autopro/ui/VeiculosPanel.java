package br.com.autopro.ui;

import br.com.autopro.model.SituacaoRevisao;
import br.com.autopro.model.Veiculo;
import br.com.autopro.ui.componentes.DialogoConsulta;
import br.com.autopro.ui.componentes.ModeloTabela;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Revisao;
import br.com.autopro.util.Formatador;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

public class VeiculosPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final JTextField busca = UiUtil.campoBusca("Pesquisar por placa, modelo ou proprietário", this::atualizar);
    private final JCheckBox somentePendentes = new JCheckBox("Somente revisão vencida / sem histórico");
    private final Tabela<Veiculo> tabela;

    public VeiculosPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        somentePendentes.setOpaque(false);
        somentePendentes.addActionListener(e -> atualizar());

        ModeloTabela<Veiculo> modelo = new ModeloTabela<Veiculo>()
                .coluna("ID", Integer.class, Veiculo::getId)
                .coluna("Placa", Veiculo::getPlaca)
                .coluna("Modelo", Veiculo::getModelo)
                .coluna("Ano", Integer.class, Veiculo::getAno)
                .coluna("Proprietário", v -> FabricaTabelas.nomeComVip(v.getClienteNome(), v.isProprietarioVip()))
                .coluna("Km atual", Integer.class, Veiculo::getKmAtual)
                .coluna("Última revisão", LocalDate.class, Veiculo::getDataUltimaRevisao)
                .coluna("Km últ. revisão", Integer.class, Veiculo::getKmUltimaRevisao)
                .coluna("Revisão", v -> situacao(v));
        tabela = new Tabela<>(modelo)
                .destacar(v -> situacao(v).requerAtencao(), Tema.DESTAQUE_ALERTA)
                .larguras(45, 85, 190, 55, 210, 90, 105, 105, 170);
        tabela.aoDuploClique(this::editar);

        add(UiUtil.cabecalho("Veículos", "Revisão recomendada a cada 10.000 km ou 6 meses, o que ocorrer primeiro. "
                + "Linhas em vermelho precisam de revisão."), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(UiUtil.linhaBotoes(FlowLayout.LEFT, busca, UiUtil.botao("Pesquisar", e -> atualizar()),
                somentePendentes), BorderLayout.NORTH);
        centro.add(tabela.comRolagem(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Novo veículo", e -> novo()),
                UiUtil.botao("Editar", e -> editar()),
                UiUtil.botao("Atualizar km", e -> atualizarKm()),
                UiUtil.botao("Registrar revisão", e -> registrarRevisao()),
                UiUtil.botao("Histórico de manutenção", e -> historico()),
                UiUtil.botaoPerigo("Excluir", e -> excluir())), BorderLayout.SOUTH);
    }

    private SituacaoRevisao situacao(Veiculo v) {
        return ctx.veiculos().situacaoRevisao(v);
    }

    @Override
    public void atualizar() {
        UiUtil.executar(this, () -> tabela.setDados(somentePendentes.isSelected()
                ? ctx.veiculos().listarComRevisaoPendente(busca.getText())
                : ctx.veiculos().listar(busca.getText())));
    }

    private void novo() {
        if (new VeiculoDialog(this, ctx, null).exibir()) {
            atualizar();
        }
    }

    private void editar() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um veículo").ifPresent(v -> {
            if (new VeiculoDialog(this, ctx, v).exibir()) {
                atualizar();
            }
        });
    }

    private void atualizarKm() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um veículo").ifPresent(v -> {
            String texto = JOptionPane.showInputDialog(this, "Nova quilometragem do veículo " + v.getPlaca()
                    + "\n(atual: " + Formatador.inteiro(v.getKmAtual()) + " km)", v.getKmAtual());
            if (texto != null && UiUtil.executar(this, () ->
                    ctx.veiculos().atualizarKm(v.getId(), Formatador.parseInteiro(texto, "Quilometragem")))) {
                UiUtil.sucesso(this, "Quilometragem atualizada.");
                atualizar();
            }
        });
    }

    private void registrarRevisao() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um veículo").ifPresent(v -> {
            if (new RevisaoDialog(this, ctx, v).exibir()) {
                atualizar();
            }
        });
    }

    private void excluir() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um veículo").ifPresent(v -> {
            if (UiUtil.confirmar(this, "Excluir o veículo " + v.getPlaca() + " - " + v.getModelo() + "?")
                    && UiUtil.executar(this, () -> ctx.veiculos().excluir(v.getId()))) {
                UiUtil.sucesso(this, "Veículo excluído.");
                atualizar();
            }
        });
    }

    private void historico() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um veículo").ifPresent(v -> mostrarHistorico(this, ctx, v));
    }

    /** Diálogo com ordens, peças utilizadas e revisões do veículo (reutilizado em outros módulos). */
    static void mostrarHistorico(java.awt.Component pai, ContextoApp ctx, Veiculo v) {
        UiUtil.executar(pai, () -> {
            List<OrdemServico> ordens = ctx.veiculos().ordens(v.getId());
            List<ItemOrdem> pecas = ctx.veiculos().pecasUtilizadas(v.getId());
            List<Revisao> revisoes = ctx.veiculos().revisoes(v.getId());
            Tabela<OrdemServico> tOrdens = FabricaTabelas.ordens();
            tOrdens.setDados(ordens);
            Tabela<ItemOrdem> tPecas = FabricaTabelas.itens();
            tPecas.setDados(pecas);
            Tabela<Revisao> tRevisoes = FabricaTabelas.revisoes();
            tRevisoes.setDados(revisoes);
            new DialogoConsulta(pai, "Histórico do veículo " + v.getPlaca(),
                    v.getModelo() + " (" + v.getAno() + ") - " + v.getClienteNome() + " - "
                            + Formatador.inteiro(v.getKmAtual()) + " km - Revisão: "
                            + ctx.veiculos().situacaoRevisao(v).getDescricao())
                    .aba("Ordens de serviço (" + ordens.size() + ")", tOrdens.comRolagem())
                    .aba("Peças utilizadas (" + pecas.size() + ")", tPecas.comRolagem())
                    .aba("Revisões (" + revisoes.size() + ")", tRevisoes.comRolagem())
                    .exibir();
        });
    }
}
