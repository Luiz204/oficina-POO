package br.com.autopro.ui;

import br.com.autopro.model.MovimentacaoEstoque;
import br.com.autopro.model.Peca;
import br.com.autopro.ui.componentes.DialogoConsulta;
import br.com.autopro.ui.componentes.ModeloTabela;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.util.List;

public class EstoquePanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final JTextField busca = UiUtil.campoBusca("Pesquisar por código ou nome", this::atualizar);
    private final JCheckBox somenteAbaixo = new JCheckBox("Somente abaixo do mínimo");
    private final JLabel alerta = new JLabel();
    private final Tabela<Peca> tabela;

    public EstoquePanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        somenteAbaixo.setOpaque(false);
        somenteAbaixo.addActionListener(e -> atualizar());
        alerta.setFont(Tema.fonte(Font.BOLD, 13));
        alerta.setForeground(Tema.PERIGO);

        ModeloTabela<Peca> modelo = new ModeloTabela<Peca>()
                .coluna("ID", Integer.class, Peca::getId)
                .coluna("Código", Peca::getCodigo)
                .coluna("Nome", Peca::getNome)
                .coluna("Descrição", Peca::getDescricao)
                .coluna("Preço unit.", BigDecimal.class, Peca::getPrecoUnitario)
                .coluna("Disponível", Integer.class, Peca::getQuantidade)
                .coluna("Mínimo", Integer.class, Peca::getEstoqueMinimo)
                .coluna("Situação", p -> p.isAbaixoDoMinimo() ? "Abaixo do mínimo" : "OK");
        tabela = new Tabela<>(modelo).destacar(Peca::isAbaixoDoMinimo, Tema.DESTAQUE_ALERTA)
                .larguras(45, 120, 220, 260, 95, 85, 70, 150);
        tabela.aoDuploClique(this::editar);

        add(UiUtil.cabecalho("Estoque de peças", "Entradas, saídas por ordem de serviço e alertas de estoque mínimo."),
                BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        JPanel filtros = new JPanel(new BorderLayout());
        filtros.setOpaque(false);
        filtros.add(UiUtil.linhaBotoes(FlowLayout.LEFT, busca, UiUtil.botao("Pesquisar", e -> atualizar()),
                somenteAbaixo), BorderLayout.WEST);
        filtros.add(alerta, BorderLayout.EAST);
        centro.add(filtros, BorderLayout.NORTH);
        centro.add(tabela.comRolagem(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Nova peça", e -> nova()),
                UiUtil.botao("Editar (preço/quantidade)", e -> editar()),
                UiUtil.botaoSucesso("Registrar entrada", e -> entrada()),
                UiUtil.botao("Histórico de utilização", e -> historico()),
                UiUtil.botaoPerigo("Excluir", e -> excluir())), BorderLayout.SOUTH);
    }

    @Override
    public void atualizar() {
        UiUtil.executar(this, () -> {
            tabela.setDados(ctx.estoque().listar(busca.getText(), somenteAbaixo.isSelected()));
            int abaixo = ctx.estoque().listarAbaixoMinimo().size();
            alerta.setText(abaixo == 0 ? "" : "Atenção: " + abaixo + " peça(s) abaixo do estoque mínimo  ");
        });
    }

    private void nova() {
        if (new PecaDialog(this, ctx, null).exibir()) {
            atualizar();
        }
    }

    private void editar() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "uma peça").ifPresent(p -> {
            if (new PecaDialog(this, ctx, p).exibir()) {
                atualizar();
            }
        });
    }

    private void entrada() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "uma peça").ifPresent(p -> {
            JSpinner quantidade = new JSpinner(new SpinnerNumberModel(1, 1, 100_000, 1));
            JTextField obs = new JTextField(24);
            obs.putClientProperty("JTextField.placeholderText", "Ex.: NF 1234 - fornecedor");
            JPanel form = new br.com.autopro.ui.componentes.Formulario()
                    .campo("Peça", new JLabel(p.getCodigo() + " - " + p.getNome()))
                    .campo("Disponível", new JLabel(String.valueOf(p.getQuantidade())))
                    .campo("Quantidade de entrada", quantidade, true)
                    .campo("Observação", obs);
            int r = javax.swing.JOptionPane.showConfirmDialog(this, form, "Registrar entrada de estoque",
                    javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (r == javax.swing.JOptionPane.OK_OPTION && UiUtil.executar(this, () ->
                    ctx.estoque().registrarEntrada(p.getId(), (Integer) quantidade.getValue(), obs.getText()))) {
                UiUtil.sucesso(this, "Entrada registrada.");
                atualizar();
            }
        });
    }

    private void historico() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "uma peça").ifPresent(p -> UiUtil.executar(this, () -> {
            List<MovimentacaoEstoque> movs = ctx.estoque().historicoMovimentacoes(p.getId());
            Tabela<MovimentacaoEstoque> t = FabricaTabelas.movimentacoes();
            t.setDados(movs);
            long usosEmOrdens = movs.stream().filter(m -> m.getOrdemId() != null).count();
            new DialogoConsulta(this, "Histórico da peça " + p.getCodigo(), p.getNome() + " - preço atual "
                    + Formatador.moeda(p.getPrecoUnitario()) + " - " + usosEmOrdens
                    + " saída(s) vinculada(s) a ordens de serviço (preço preservado no momento do serviço)")
                    .aba("Movimentações", t.comRolagem())
                    .exibir();
        }));
    }

    private void excluir() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "uma peça").ifPresent(p -> {
            if (UiUtil.confirmar(this, "Excluir a peça " + p.getCodigo() + " - " + p.getNome() + "?")
                    && UiUtil.executar(this, () -> ctx.estoque().excluir(p.getId()))) {
                UiUtil.sucesso(this, "Peça excluída.");
                atualizar();
            }
        });
    }
}
