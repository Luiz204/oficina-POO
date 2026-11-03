package br.com.autopro.ui;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.Cliente;
import br.com.autopro.model.Veiculo;
import br.com.autopro.ui.componentes.CampoData;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.List;

public class VeiculoDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient Veiculo veiculo;
    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JTextField modelo = new JTextField(26);
    private final JTextField ano = new JTextField(6);
    private final JTextField placa = new JTextField(10);
    private final JTextField kmAtual = new JTextField(10);
    private final CampoData dataUltimaRevisao = new CampoData();
    private final JTextField kmUltimaRevisao = new JTextField(10);

    public VeiculoDialog(Component pai, ContextoApp ctx, Veiculo existente) {
        super(pai, existente == null ? "Novo veículo" : "Editar veículo");
        this.ctx = ctx;
        this.veiculo = existente != null ? existente : new Veiculo();
        List<Cliente> clientes = ctx.clientes().listarTodos();
        clientes.forEach(cliente::addItem);
        placa.putClientProperty("JTextField.placeholderText", "ABC1D23");

        Formulario f = new Formulario()
                .campo("Proprietário", cliente, true)
                .campo("Modelo", modelo, true)
                .campo("Ano", ano, true)
                .campo("Placa", placa, true)
                .campo("Quilometragem atual", kmAtual, true)
                .campo("Data da última revisão", dataUltimaRevisao)
                .campo("Km da última revisão", kmUltimaRevisao);
        if (existente != null) {
            clientes.stream().filter(c -> c.getId().equals(existente.getClienteId())).findFirst()
                    .ifPresent(cliente::setSelectedItem);
            modelo.setText(existente.getModelo());
            ano.setText(String.valueOf(existente.getAno()));
            placa.setText(existente.getPlaca());
            kmAtual.setText(String.valueOf(existente.getKmAtual()));
            dataUltimaRevisao.setData(existente.getDataUltimaRevisao());
            kmUltimaRevisao.setText(existente.getKmUltimaRevisao() == null ? ""
                    : String.valueOf(existente.getKmUltimaRevisao()));
            dataUltimaRevisao.setEnabled(false);
            kmUltimaRevisao.setEnabled(false);
            f.linhaInteira(UiUtil.subtitulo("Os dados da última revisão são atualizados pelo registro de revisões."));
        } else {
            cliente.setSelectedIndex(-1);
            f.linhaInteira(UiUtil.subtitulo("Última revisão: opcional. Sem histórico, o veículo será sinalizado "
                    + "para avaliação."));
        }
        montar(f);
    }

    @Override
    protected boolean salvar() {
        Cliente c = (Cliente) cliente.getSelectedItem();
        veiculo.setClienteId(c == null ? null : c.getId());
        veiculo.setModelo(modelo.getText());
        veiculo.setAno(Formatador.parseInteiro(ano.getText(), "Ano"));
        veiculo.setPlaca(placa.getText());
        veiculo.setKmAtual(Formatador.parseInteiro(kmAtual.getText(), "Quilometragem atual"));
        if (veiculo.getId() == null) {
            veiculo.setDataUltimaRevisao(dataUltimaRevisao.getData("Data da última revisão"));
            String km = kmUltimaRevisao.getText();
            veiculo.setKmUltimaRevisao(km == null || km.isBlank() ? null
                    : Formatador.parseInteiro(km, "Km da última revisão"));
        }
        if (c == null) {
            throw new NegocioException("Selecione o proprietário do veículo.");
        }
        ctx.veiculos().salvar(veiculo);
        UiUtil.sucesso(this, "Veículo salvo com sucesso.");
        return true;
    }
}
