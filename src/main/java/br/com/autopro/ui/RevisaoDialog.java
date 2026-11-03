package br.com.autopro.ui;

import br.com.autopro.model.Revisao;
import br.com.autopro.model.Veiculo;
import br.com.autopro.ui.componentes.CampoData;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.time.LocalDate;

/** Registro de revisão periódica de um veículo. */
public class RevisaoDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient Veiculo veiculo;
    private final CampoData data = new CampoData();
    private final JTextField km = new JTextField(10);
    private final JTextArea observacoes = new JTextArea(4, 30);

    public RevisaoDialog(Component pai, ContextoApp ctx, Veiculo veiculo) {
        super(pai, "Registrar revisão");
        this.ctx = ctx;
        this.veiculo = veiculo;
        data.setData(LocalDate.now());
        km.setText(String.valueOf(veiculo.getKmAtual()));
        observacoes.setLineWrap(true);
        observacoes.setWrapStyleWord(true);
        String ultima = veiculo.possuiHistoricoRevisao()
                ? Formatador.data(veiculo.getDataUltimaRevisao()) + " com "
                + Formatador.inteiro(veiculo.getKmUltimaRevisao()) + " km"
                : "sem histórico";
        montar(new Formulario()
                .campo("Veículo", new JLabel(veiculo.getPlaca() + " - " + veiculo.getModelo()))
                .campo("Última revisão", new JLabel(ultima))
                .campo("Data da revisão", data, true)
                .campo("Quilometragem", km, true)
                .campo("Observações", new JScrollPane(observacoes)));
    }

    @Override
    protected boolean salvar() {
        Revisao r = new Revisao(veiculo.getId(), data.getData("Data da revisão"),
                Formatador.parseInteiro(km.getText(), "Quilometragem"), observacoes.getText());
        if (r.getDataRevisao() == null) {
            throw new br.com.autopro.exception.NegocioException("Informe a data da revisão.");
        }
        ctx.revisoes().registrar(r);
        UiUtil.sucesso(this, "Revisão registrada. Os dados de última revisão do veículo foram atualizados.");
        return true;
    }
}
