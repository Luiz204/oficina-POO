package br.com.autopro.ui;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.Agendamento;
import br.com.autopro.model.TipoAgendamento;
import br.com.autopro.model.Veiculo;
import br.com.autopro.ui.componentes.CampoData;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalTime;

public class AgendamentoDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient Agendamento agendamento;
    private final JComboBox<Veiculo> veiculo = new JComboBox<>();
    private final CampoData dataHora = new CampoData(true);
    private final JComboBox<TipoAgendamento> tipo = new JComboBox<>(TipoAgendamento.values());
    private final JTextField duracao = new JTextField(6);
    private final JTextField descricao = new JTextField(30);

    public AgendamentoDialog(Component pai, ContextoApp ctx, Agendamento existente, Integer veiculoInicial,
                             TipoAgendamento tipoInicial) {
        super(pai, existente == null ? "Novo agendamento" : "Editar agendamento");
        this.ctx = ctx;
        this.agendamento = existente != null ? existente : new Agendamento();
        ctx.veiculos().listarTodos().forEach(veiculo::addItem);
        veiculo.setSelectedIndex(-1);
        Integer selecionar = existente != null ? existente.getVeiculoId() : veiculoInicial;
        for (int i = 0; i < veiculo.getItemCount(); i++) {
            if (veiculo.getItemAt(i).getId().equals(selecionar)) {
                veiculo.setSelectedIndex(i);
            }
        }
        if (existente != null) {
            dataHora.setDataHora(existente.getDataHora());
            tipo.setSelectedItem(existente.getTipo());
            duracao.setText(existente.getDuracaoMinutos() == null ? "" : existente.getDuracaoMinutos().toString());
            descricao.setText(existente.getDescricao());
        } else {
            dataHora.setDataHora(LocalDate.now().plusDays(1).atTime(LocalTime.of(8, 0)));
            tipo.setSelectedItem(tipoInicial != null ? tipoInicial : TipoAgendamento.SERVICO);
        }
        String duracaoRapida = ctx.configuracao().duracaoRevisaoRapida().map(m -> m + " min")
                .orElse("não configurada");
        montar(new Formulario()
                .campo("Veículo", veiculo, true)
                .campo("Data e hora", dataHora, true)
                .campo("Tipo", tipo, true)
                .campo("Duração estimada (min)", duracao)
                .campo("Descrição", descricao)
                .linhaInteira(UiUtil.subtitulo("Revisão rápida sem duração informada usa o parâmetro configurado ("
                        + duracaoRapida + ").")));
    }

    @Override
    protected boolean salvar() {
        Veiculo v = (Veiculo) veiculo.getSelectedItem();
        if (v == null) {
            throw new NegocioException("Selecione o veículo.");
        }
        agendamento.setVeiculoId(v.getId());
        agendamento.setDataHora(dataHora.getDataHora("Data e hora"));
        agendamento.setTipo((TipoAgendamento) tipo.getSelectedItem());
        String d = duracao.getText();
        agendamento.setDuracaoMinutos(d == null || d.isBlank() ? null : Formatador.parseInteiro(d, "Duração"));
        agendamento.setDescricao(descricao.getText());
        ctx.agendamentos().salvar(agendamento);
        UiUtil.sucesso(this, "Agendamento salvo para " + Formatador.dataHora(agendamento.getDataHora()) + ".");
        return true;
    }
}
