package br.com.autopro.ui;

import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Pagamento;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class PagamentosPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final Tabela<OrdemServico> pendentes = FabricaTabelas.ordens();
    private final Tabela<Pagamento> realizados = FabricaTabelas.pagamentos();
    private final JTextField busca = UiUtil.campoBusca("Cliente, placa ou nº da OS", this::atualizar);

    public PagamentosPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        add(UiUtil.cabecalho("Pagamentos", "Pagamento único por ordem, registrado quando o veículo está pronto. "
                + "A entrega só é liberada após o pagamento."), BorderLayout.NORTH);
        pendentes.aoDuploClique(this::registrar);

        JPanel topo = new JPanel(new BorderLayout(0, 4));
        topo.setOpaque(false);
        topo.add(UiUtil.secao("Ordens prontas aguardando pagamento"), BorderLayout.NORTH);
        topo.add(pendentes.comRolagem(), BorderLayout.CENTER);
        topo.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Registrar pagamento", e -> registrar())), BorderLayout.SOUTH);

        JPanel baixo = new JPanel(new BorderLayout(0, 4));
        baixo.setOpaque(false);
        JPanel cab = new JPanel(new BorderLayout());
        cab.setOpaque(false);
        cab.add(UiUtil.secao("Pagamentos registrados"), BorderLayout.WEST);
        cab.add(UiUtil.linhaBotoes(FlowLayout.RIGHT, busca, UiUtil.botao("Pesquisar", e -> atualizar()),
                UiUtil.botao("Histórico da ordem", e -> historico())), BorderLayout.EAST);
        baixo.add(cab, BorderLayout.NORTH);
        baixo.add(realizados.comRolagem(), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topo, baixo);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void atualizar() {
        UiUtil.executar(this, () -> {
            pendentes.setDados(ctx.ordens().listarAguardandoPagamento());
            realizados.setDados(ctx.pagamentos().listar(busca.getText()));
        });
    }

    private void registrar() {
        UiUtil.exigirSelecao(this, pendentes.getSelecionado(), "uma ordem aguardando pagamento").ifPresent(o -> {
            if (new PagamentoDialog(this, ctx, o).exibir()) {
                atualizar();
            }
        });
    }

    private void historico() {
        UiUtil.exigirSelecao(this, realizados.getSelecionado(), "um pagamento")
                .ifPresent(p -> OrdensPanel.mostrarHistorico(this, ctx, p.getOrdemId()));
    }
}
