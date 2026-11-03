package br.com.autopro.ui;

import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.Cliente;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.UiUtil;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Component;

public class ClienteDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient Cliente cliente;
    private final JTextField nome = new JTextField(30);
    private final JTextField telefone = new JTextField(16);
    private final JTextField email = new JTextField(30);
    private final JComboBox<CategoriaCliente> categoria = new JComboBox<>(CategoriaCliente.values());

    public ClienteDialog(Component pai, ContextoApp ctx, Cliente existente) {
        super(pai, existente == null ? "Novo cliente" : "Editar cliente");
        this.ctx = ctx;
        this.cliente = existente != null ? existente : new Cliente();
        telefone.putClientProperty("JTextField.placeholderText", "(11) 98765-4321");
        email.putClientProperty("JTextField.placeholderText", "opcional");
        if (existente != null) {
            nome.setText(existente.getNome());
            telefone.setText(existente.getTelefone());
            email.setText(existente.getEmail());
            categoria.setSelectedItem(existente.getCategoria());
        }
        montar(new Formulario()
                .campo("Nome completo", nome, true)
                .campo("Telefone", telefone, true)
                .campo("E-mail", email)
                .campo("Categoria", categoria, true)
                .linhaInteira(UiUtil.subtitulo("VIP: 10% de desconto na mão de obra e prioridade no atendimento.")));
    }

    @Override
    protected boolean salvar() {
        cliente.setNome(nome.getText());
        cliente.setTelefone(telefone.getText());
        cliente.setEmail(email.getText());
        cliente.setCategoria((CategoriaCliente) categoria.getSelectedItem());
        ctx.clientes().salvar(cliente);
        UiUtil.sucesso(this, "Cliente salvo com sucesso.");
        return true;
    }
}
