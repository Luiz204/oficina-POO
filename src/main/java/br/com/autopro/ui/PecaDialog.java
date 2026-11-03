package br.com.autopro.ui;

import br.com.autopro.model.Peca;
import br.com.autopro.ui.componentes.DialogoBase;
import br.com.autopro.ui.componentes.Formulario;
import br.com.autopro.ui.componentes.UiUtil;
import br.com.autopro.util.Formatador;

import javax.swing.JTextField;
import java.awt.Component;

public class PecaDialog extends DialogoBase {

    private static final long serialVersionUID = 1L;

    private final transient ContextoApp ctx;
    private final transient Peca peca;
    private final JTextField codigo = new JTextField(14);
    private final JTextField nome = new JTextField(28);
    private final JTextField descricao = new JTextField(28);
    private final JTextField preco = new JTextField(10);
    private final JTextField quantidade = new JTextField(8);
    private final JTextField minimo = new JTextField(8);

    public PecaDialog(Component pai, ContextoApp ctx, Peca existente) {
        super(pai, existente == null ? "Nova peça" : "Editar peça");
        this.ctx = ctx;
        this.peca = existente != null ? existente : new Peca();
        preco.putClientProperty("JTextField.placeholderText", "0,00");
        Formulario f = new Formulario()
                .campo("Código único", codigo, true)
                .campo("Nome da peça", nome, true)
                .campo("Descrição", descricao)
                .campo("Preço unitário (R$)", preco, true)
                .campo("Quantidade disponível", quantidade, true)
                .campo("Estoque mínimo", minimo, true);
        if (existente != null) {
            codigo.setText(existente.getCodigo());
            nome.setText(existente.getNome());
            descricao.setText(existente.getDescricao());
            preco.setText(Formatador.decimal(existente.getPrecoUnitario()));
            quantidade.setText(String.valueOf(existente.getQuantidade()));
            minimo.setText(String.valueOf(existente.getEstoqueMinimo()));
            f.linhaInteira(UiUtil.subtitulo("Alterações de quantidade são registradas como ajuste de inventário. "
                    + "O novo preço não altera ordens já registradas."));
        } else {
            quantidade.setText("0");
            minimo.setText("0");
        }
        montar(f);
    }

    @Override
    protected boolean salvar() {
        peca.setCodigo(codigo.getText());
        peca.setNome(nome.getText());
        peca.setDescricao(descricao.getText());
        peca.setPrecoUnitario(Formatador.parseMoeda(preco.getText(), "Preço unitário"));
        peca.setQuantidade(Formatador.parseInteiro(quantidade.getText(), "Quantidade disponível"));
        peca.setEstoqueMinimo(Formatador.parseInteiro(minimo.getText(), "Estoque mínimo"));
        ctx.estoque().salvar(peca);
        UiUtil.sucesso(this, "Peça salva com sucesso.");
        return true;
    }
}
