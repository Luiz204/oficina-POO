package br.com.autopro.ui;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.model.Peca;
import br.com.autopro.model.ValoresOrdem;
import br.com.autopro.model.Veiculo;
import br.com.autopro.service.CalculadoraOrdem;
import br.com.autopro.ui.componentes.CampoData;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.ModeloTabela;
import br.com.autopro.ui.componentes.Tabela;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Criação e edição de ordem de serviço, com cálculo automático dos valores. */
public class OrdemDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient OrdemServico existente;
    private transient OrdemServico salva;
    private final transient Map<Integer, Peca> pecasPorId = new HashMap<>();
    private final transient Map<Integer, BigDecimal> precosOriginais = new HashMap<>();
    private final transient List<ItemOrdem> itens = new ArrayList<>();

    private final JComboBox<Veiculo> veiculo = new JComboBox<>();
    private final JLabel infoCliente = new JLabel(" ");
    private final JTextArea descricao = new JTextArea(4, 40);
    private final JTextField horas = new JTextField(6);
    private final CampoData dataAgendada = new CampoData();
    private final JComboBox<Peca> pecaCombo = new JComboBox<>();
    private final JSpinner quantidade = new JSpinner(new SpinnerNumberModel(1, 1, 10_000, 1));
    private final Tabela<ItemOrdem> tabelaItens;
    private final JLabel lPecas = valor();
    private final JLabel lMaoObra = valor();
    private final JLabel lDesconto = valor();
    private final JLabel lTotal = valor();

    private final boolean servicoEditavel;
    private final boolean itensEditaveis;

    /**
     * @param existente ordem completa (com itens) para edição, ou null para nova
     * @param veiculoInicial veículo pré-selecionado (nova ordem a partir de agendamento), ou null
     */
    public OrdemDialog(Component pai, ContextoApp ctx, OrdemServico existente, Integer veiculoInicial,
                       LocalDate dataInicial, String descricaoInicial) {
        super(pai, existente == null ? "Nova ordem de serviço" : "Ordem de serviço #" + existente.getId());
        this.ctx = ctx;
        this.existente = existente;
        this.servicoEditavel = existente == null || existente.getStatus().permiteAlterarServico();
        this.itensEditaveis = existente == null
                || (existente.getStatus().permiteAlterarItens() && !existente.isEstoqueBaixado());

        for (Peca p : ctx.estoque().listarTodas()) {
            pecasPorId.put(p.getId(), p);
            pecaCombo.addItem(p);
        }
        ModeloTabela<ItemOrdem> mItens = new ModeloTabela<ItemOrdem>()
                .coluna("Código", ItemOrdem::getPecaCodigo)
                .coluna("Peça", ItemOrdem::getPecaNome)
                .coluna("Qtd.", Integer.class, ItemOrdem::getQuantidade)
                .coluna("Preço unit.", BigDecimal.class, ItemOrdem::getPrecoUnitario)
                .coluna("Subtotal", BigDecimal.class, ItemOrdem::getSubtotal)
                .coluna("Em estoque", Integer.class, i -> pecasPorId.containsKey(i.getPecaId())
                        ? pecasPorId.get(i.getPecaId()).getQuantidade() : null);
        tabelaItens = new Tabela<>(mItens)
                .destacar(i -> itensEditaveis && pecasPorId.containsKey(i.getPecaId())
                        && !pecasPorId.get(i.getPecaId()).possuiQuantidade(i.getQuantidade()), Tema.DESTAQUE_ALERTA)
                .larguras(110, 260, 50, 100, 100, 85);

        if (existente == null) {
            ctx.veiculos().listarTodos().forEach(veiculo::addItem);
            veiculo.setSelectedIndex(-1);
            if (veiculoInicial != null) {
                for (int i = 0; i < veiculo.getItemCount(); i++) {
                    if (veiculo.getItemAt(i).getId().equals(veiculoInicial)) {
                        veiculo.setSelectedIndex(i);
                    }
                }
            }
            horas.setText("1,0");
            dataAgendada.setData(dataInicial);
            descricao.setText(descricaoInicial == null ? "" : descricaoInicial);
        } else {
            Veiculo v = ctx.veiculos().buscar(existente.getVeiculoId());
            veiculo.addItem(v);
            veiculo.setEnabled(false);
            descricao.setText(existente.getDescricao());
            horas.setText(Formatador.decimal(existente.getHorasMaoObra()));
            dataAgendada.setData(existente.getDataAgendada());
            for (ItemOrdem i : existente.getItens()) {
                precosOriginais.put(i.getPecaId(), i.getPrecoUnitario());
                itens.add(new ItemOrdem(i.getPecaId(), i.getPecaCodigo(), i.getPecaNome(), i.getQuantidade(),
                        i.getPrecoUnitario()));
            }
        }
        veiculo.addActionListener(e -> recalcular());
        horas.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcular(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcular(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcular(); }
        });
        descricao.setLineWrap(true);
        descricao.setWrapStyleWord(true);
        descricao.setEnabled(servicoEditavel);
        horas.setEnabled(servicoEditavel);
        dataAgendada.setEnabled(servicoEditavel);
        if (!servicoEditavel) {
            botaoSalvar.setEnabled(false);
            botaoCancelar.setText("Fechar");
        }
        montar(criarConteudo());
        tabelaItens.setDados(itens);
        recalcular();
    }

    private static JLabel valor() {
        JLabel l = new JLabel("-");
        l.setFont(Tema.fonte(Font.BOLD, 14));
        return l;
    }

    private JPanel criarConteudo() {
        JScrollPane spDescricao = new JScrollPane(descricao);
        Formulario dados = new Formulario()
                .campo("Veículo", veiculo, true)
                .campo("Cliente", infoCliente)
                .campo("Defeito / serviço solicitado", spDescricao, true)
                .campo("Horas de mão de obra", horas, true)
                .campo("Data agendada", dataAgendada);
        if (existente != null) {
            dados.linhaInteira(UiUtil.subtitulo("Status: " + existente.getStatus().getDescricao()
                    + " - aberta em " + Formatador.dataHora(existente.getDataAbertura())
                    + (itensEditaveis ? "" : " - peças bloqueadas (estoque já baixado)")));
        }

        JButton adicionar = UiUtil.botao("Adicionar peça", e -> adicionarItem());
        JButton remover = UiUtil.botao("Remover selecionada", e -> removerItem());
        pecaCombo.setEnabled(itensEditaveis);
        quantidade.setEnabled(itensEditaveis);
        adicionar.setEnabled(itensEditaveis);
        remover.setEnabled(itensEditaveis);
        pecaCombo.setPreferredSize(new Dimension(380, pecaCombo.getPreferredSize().height));

        JPanel painelItens = new JPanel(new BorderLayout(0, 6));
        painelItens.setOpaque(false);
        painelItens.add(UiUtil.linhaBotoes(FlowLayout.LEFT, new JLabel("Peça:"), pecaCombo, new JLabel("Qtd.:"),
                quantidade, adicionar, remover), BorderLayout.NORTH);
        JScrollPane spItens = tabelaItens.comRolagem();
        spItens.setPreferredSize(new Dimension(720, 170));
        painelItens.add(spItens, BorderLayout.CENTER);
        painelItens.setBorder(BorderFactory.createTitledBorder("Peças necessárias"));

        JPanel totais = new JPanel(new GridLayout(2, 4, 12, 2));
        totais.setOpaque(false);
        totais.add(UiUtil.subtitulo("Peças"));
        totais.add(UiUtil.subtitulo("Mão de obra (R$ 120,00/h)"));
        totais.add(UiUtil.subtitulo("Desconto VIP (10% mão de obra)"));
        totais.add(UiUtil.subtitulo("Total"));
        totais.add(lPecas);
        totais.add(lMaoObra);
        totais.add(lDesconto);
        totais.add(lTotal);
        lTotal.setForeground(Tema.PRIMARIA);
        lTotal.setFont(Tema.fonte(Font.BOLD, 18));
        totais.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

        JPanel raiz = new JPanel(new BorderLayout(0, 6));
        raiz.add(dados, BorderLayout.NORTH);
        raiz.add(painelItens, BorderLayout.CENTER);
        raiz.add(totais, BorderLayout.SOUTH);
        return raiz;
    }

    private void adicionarItem() {
        Peca p = (Peca) pecaCombo.getSelectedItem();
        if (p == null) {
            UiUtil.aviso(this, "Selecione uma peça.");
            return;
        }
        int qtd = (Integer) quantidade.getValue();
        for (ItemOrdem i : itens) {
            if (i.getPecaId().equals(p.getId())) {
                i.setQuantidade(i.getQuantidade() + qtd);
                tabelaItens.setDados(itens);
                recalcular();
                return;
            }
        }
        BigDecimal preco = precosOriginais.getOrDefault(p.getId(), p.getPrecoUnitario());
        itens.add(new ItemOrdem(p.getId(), p.getCodigo(), p.getNome(), qtd, preco));
        tabelaItens.setDados(itens);
        recalcular();
    }

    private void removerItem() {
        ItemOrdem sel = tabelaItens.getSelecionado();
        if (sel == null) {
            UiUtil.aviso(this, "Selecione a peça a remover.");
            return;
        }
        itens.remove(sel);
        tabelaItens.setDados(itens);
        recalcular();
    }

    private CategoriaCliente categoria() {
        if (existente != null) {
            return existente.getClienteCategoria();
        }
        Veiculo v = (Veiculo) veiculo.getSelectedItem();
        return v == null ? CategoriaCliente.COMUM : v.getClienteCategoria();
    }

    /** Pré-visualização dos valores; o cálculo definitivo é refeito pelo serviço ao salvar. */
    private void recalcular() {
        Veiculo v = (Veiculo) veiculo.getSelectedItem();
        if (existente != null) {
            infoCliente.setText(FabricaTabelas.nomeComVip(existente.getClienteNome(), existente.isClienteVip())
                    + (existente.isClienteVip() ? "  (VIP - 10% na mão de obra)" : ""));
        } else {
            infoCliente.setText(v == null ? "Selecione o veículo"
                    : FabricaTabelas.nomeComVip(v.getClienteNome(), v.isProprietarioVip())
                    + (v.isProprietarioVip() ? "  (VIP - 10% na mão de obra)" : ""));
        }
        try {
            BigDecimal h = Formatador.parseDecimal(horas.getText(), "Horas");
            ValoresOrdem valores = CalculadoraOrdem.calcular(itens, h, categoria());
            lPecas.setText(Formatador.moeda(valores.getValorPecas()));
            lMaoObra.setText(Formatador.moeda(valores.getValorMaoObra()));
            lDesconto.setText("- " + Formatador.moeda(valores.getDesconto()));
            lTotal.setText(Formatador.moeda(valores.getValorTotal()));
        } catch (NegocioException e) {
            lMaoObra.setText("horas inválidas");
            lTotal.setText("-");
        }
    }

    @Override
    protected boolean salvar() {
        Veiculo v = (Veiculo) veiculo.getSelectedItem();
        if (v == null) {
            throw new NegocioException("Selecione o veículo.");
        }
        OrdemServico dados = new OrdemServico();
        dados.setId(existente == null ? null : existente.getId());
        dados.setVeiculoId(v.getId());
        dados.setDescricao(descricao.getText());
        dados.setHorasMaoObra(Formatador.parseDecimal(horas.getText(), "Horas de mão de obra"));
        dados.setDataAgendada(dataAgendada.getData("Data agendada"));
        List<ItemOrdem> copia = new ArrayList<>();
        for (ItemOrdem i : itens) {
            copia.add(new ItemOrdem(i.getPecaId(), i.getPecaCodigo(), i.getPecaNome(), i.getQuantidade(),
                    i.getPrecoUnitario()));
        }
        dados.setItens(copia);
        salva = ctx.ordens().salvar(dados);
        UiUtil.sucesso(this, "OS #" + salva.getId() + " salva. Valor total: " + Formatador.moeda(salva.getValorTotal())
                + (salva.isClienteVip() ? " (com desconto VIP de " + Formatador.moeda(salva.getDesconto()) + ")" : ""));
        return true;
    }

    public OrdemServico getOrdemSalva() {
        return salva;
    }
}
