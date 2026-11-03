package br.com.autopro.ui;

import br.com.autopro.model.Cliente;
import br.com.autopro.ui.componentes.ModeloTabela;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.ui.componentes.DialogoConsulta;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Veiculo;
import br.com.autopro.ui.componentes.TabelaPadrao;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class ClientesPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final JTextField busca = UiUtil.campoBusca("Pesquisar por nome ou telefone", this::atualizar);
    private final JCheckBox somenteVip = new JCheckBox("Somente VIP");
    private final Tabela<Cliente> tabela;

    public ClientesPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        somenteVip.setOpaque(false);
        somenteVip.addActionListener(e -> atualizar());

        ModeloTabela<Cliente> modelo = new ModeloTabela<Cliente>()
                .coluna("ID", Integer.class, Cliente::getId)
                .coluna("Nome completo", c -> FabricaTabelas.nomeComVip(c.getNome(), c.isVip()))
                .coluna("Telefone", Cliente::getTelefone)
                .coluna("E-mail", Cliente::getEmail)
                .coluna("Categoria", Cliente::getCategoria);
        tabela = new Tabela<>(modelo).destacar(Cliente::isVip, Tema.DESTAQUE_VIP).larguras(50, 320, 150, 260, 90);
        tabela.aoDuploClique(this::editar);

        add(UiUtil.cabecalho("Clientes", "Clientes VIP (linhas em amarelo) têm 10% de desconto na mão de obra "
                + "e prioridade no atendimento."), BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        centro.add(UiUtil.linhaBotoes(FlowLayout.LEFT, busca, UiUtil.botao("Pesquisar", e -> atualizar()),
                somenteVip), BorderLayout.NORTH);
        centro.add(tabela.comRolagem(), BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                UiUtil.botaoPrimario("Novo cliente", e -> novo()),
                UiUtil.botao("Editar", e -> editar()),
                UiUtil.botao("Histórico de serviços", e -> historico()),
                UiUtil.botaoPerigo("Excluir", e -> excluir())), BorderLayout.SOUTH);
    }

    @Override
    public void atualizar() {
        UiUtil.executar(this, () -> tabela.setDados(ctx.clientes().listar(busca.getText(), somenteVip.isSelected())));
    }

    private void novo() {
        if (new ClienteDialog(this, ctx, null).exibir()) {
            atualizar();
        }
    }

    private void editar() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um cliente").ifPresent(c -> {
            if (new ClienteDialog(this, ctx, c).exibir()) {
                atualizar();
            }
        });
    }

    private void excluir() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um cliente").ifPresent(c -> {
            if (UiUtil.confirmar(this, "Excluir o cliente \"" + c.getNome() + "\"?\nEsta operação não pode ser desfeita.")
                    && UiUtil.executar(this, () -> ctx.clientes().excluir(c.getId()))) {
                UiUtil.sucesso(this, "Cliente excluído.");
                atualizar();
            }
        });
    }

    private void historico() {
        UiUtil.exigirSelecao(this, tabela.getSelecionado(), "um cliente").ifPresent(c ->
                UiUtil.executar(this, () -> {
                    List<OrdemServico> ordens = ctx.clientes().historicoServicos(c.getId());
                    List<Veiculo> veiculos = ctx.veiculos().listarPorCliente(c.getId());
                    Tabela<OrdemServico> tOrdens = FabricaTabelas.ordens();
                    tOrdens.setDados(ordens);
                    ModeloTabela<Veiculo> mv = new ModeloTabela<Veiculo>()
                            .coluna("Placa", Veiculo::getPlaca).coluna("Modelo", Veiculo::getModelo)
                            .coluna("Ano", Integer.class, Veiculo::getAno)
                            .coluna("Km atual", Integer.class, Veiculo::getKmAtual);
                    mv.setDados(veiculos);
                    new DialogoConsulta(this, "Histórico de " + c.getNome(),
                            c.getTelefone() + " - " + c.getCategoria().getDescricao() + " - "
                                    + ordens.size() + " ordem(ns) de serviço")
                            .aba("Ordens de serviço", tOrdens.comRolagem())
                            .aba("Veículos", new JScrollPane(new TabelaPadrao(mv)))
                            .exibir();
                }));
    }
}
