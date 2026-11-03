package br.com.autopro.ui;

import br.com.autopro.model.FormaPagamento;
import br.com.autopro.model.OrdemServico;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Font;

/** Registro do pagamento (único e integral) de uma ordem pronta para entrega. */
public class PagamentoDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient OrdemServico ordem;
    private final JComboBox<FormaPagamento> forma = new JComboBox<>(FormaPagamento.values());
    private final JTextField observacao = new JTextField(26);

    public PagamentoDialog(Component pai, ContextoApp ctx, OrdemServico ordem) {
        super(pai, "Registrar pagamento - OS #" + ordem.getId());
        this.ctx = ctx;
        this.ordem = ordem;
        botaoSalvar.setText("Confirmar pagamento");
        JLabel total = new JLabel(Formatador.moeda(ordem.getValorTotal()));
        total.setFont(Tema.fonte(Font.BOLD, 20));
        total.setForeground(Tema.PRIMARIA);
        montar(new Formulario()
                .campo("Ordem de serviço", new JLabel("#" + ordem.getId() + " - " + ordem.getStatus().getDescricao()))
                .campo("Cliente", new JLabel(FabricaTabelas.nomeComVip(ordem.getClienteNome(), ordem.isClienteVip())))
                .campo("Veículo", new JLabel(ordem.getPlaca() + " - " + ordem.getModeloVeiculo()))
                .campo("Peças", new JLabel(Formatador.moeda(ordem.getValorPecas())))
                .campo("Mão de obra", new JLabel(Formatador.moeda(ordem.getValorMaoObra())))
                .campo("Desconto VIP", new JLabel("- " + Formatador.moeda(ordem.getDesconto())))
                .campo("Valor devido", total)
                .campo("Forma de pagamento", forma, true)
                .campo("Observação", observacao)
                .linhaInteira(UiUtil.subtitulo("Confirmação manual pelo operador (sem integração com gateway).")));
    }

    @Override
    protected boolean salvar() {
        FormaPagamento f = (FormaPagamento) forma.getSelectedItem();
        if (!UiUtil.confirmar(this, "Confirmar o recebimento de " + Formatador.moeda(ordem.getValorTotal())
                + " via " + f + " para a OS #" + ordem.getId() + "?")) {
            return false;
        }
        ctx.pagamentos().registrar(ordem.getId(), f, observacao.getText());
        UiUtil.sucesso(this, "Pagamento registrado. O veículo já pode ser entregue.");
        return true;
    }
}
