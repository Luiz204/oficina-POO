package br.com.autopro.ui;

import br.com.autopro.model.FiltroRelatorio;
import br.com.autopro.model.Relatorio;
import br.com.autopro.model.StatusOrdem;
import br.com.autopro.model.TipoRelatorio;
import br.com.autopro.ui.componentes.CampoData;
import br.com.autopro.ui.componentes.TabelaPadrao;
import br.com.autopro.ui.componentes.Tema;
import br.com.autopro.ui.componentes.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Relatórios consultáveis com filtros e exportação para CSV. */
public class RelatoriosPanel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;
    private static final String TODOS = "Todos";

    private final transient ContextoApp ctx;
    private final JComboBox<TipoRelatorio> tipo = new JComboBox<>(TipoRelatorio.values());
    private final JTextField cliente = new JTextField(14);
    private final JTextField placa = new JTextField(8);
    private final CampoData inicio = new CampoData();
    private final CampoData fim = new CampoData();
    private final JComboBox<Object> status = new JComboBox<>();
    private final JButton exportar = UiUtil.botao("Exportar CSV", e -> exportar());
    private final DefaultTableModel modelo = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final TabelaPadrao tabela = new TabelaPadrao(modelo);
    private final JLabel resumo = new JLabel(" ");
    private transient Relatorio atual;

    public RelatoriosPanel(ContextoApp ctx) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        setBackground(Tema.FUNDO);
        status.addItem(TODOS);
        for (StatusOrdem s : StatusOrdem.values()) {
            status.addItem(s);
        }
        cliente.putClientProperty("JTextField.placeholderText", "nome");
        placa.putClientProperty("JTextField.placeholderText", "placa");
        tipo.addActionListener(e -> ajustarFiltros());
        resumo.setFont(Tema.fonte(Font.BOLD, 13));
        exportar.setEnabled(false);

        add(UiUtil.cabecalho("Relatórios", "Selecione o relatório, aplique os filtros e exporte para CSV."),
                BorderLayout.NORTH);

        JPanel filtros = new JPanel(new BorderLayout(0, 4));
        filtros.setOpaque(false);
        filtros.add(UiUtil.linhaBotoes(FlowLayout.LEFT, new JLabel("Relatório:"), tipo,
                UiUtil.botaoPrimario("Gerar", e -> gerar()), UiUtil.botao("Limpar filtros", e -> limpar()), exportar),
                BorderLayout.NORTH);
        filtros.add(UiUtil.linhaBotoes(FlowLayout.LEFT,
                new JLabel("Cliente:"), cliente, new JLabel("Placa:"), placa,
                new JLabel("De:"), inicio, new JLabel("Até:"), fim, new JLabel("Status:"), status),
                BorderLayout.CENTER);
        filtros.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.setOpaque(false);
        centro.add(filtros, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(tabela);
        sp.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        centro.add(sp, BorderLayout.CENTER);
        centro.add(resumo, BorderLayout.SOUTH);
        add(centro, BorderLayout.CENTER);
        ajustarFiltros();
    }

    private void ajustarFiltros() {
        TipoRelatorio t = (TipoRelatorio) tipo.getSelectedItem();
        cliente.setEnabled(t.filtraCliente());
        placa.setEnabled(t.filtraPlaca());
        inicio.setEnabled(t.filtraPeriodo());
        fim.setEnabled(t.filtraPeriodo());
        status.setEnabled(t.filtraStatus());
    }

    @Override
    public void atualizar() {
        if (atual != null) {
            gerar();
        }
    }

    private void limpar() {
        cliente.setText("");
        placa.setText("");
        inicio.setData(null);
        fim.setData(null);
        status.setSelectedIndex(0);
    }

    private void gerar() {
        TipoRelatorio t = (TipoRelatorio) tipo.getSelectedItem();
        UiUtil.obter(this, () -> {
            FiltroRelatorio f = new FiltroRelatorio();
            if (t.filtraCliente()) {
                f.setCliente(cliente.getText());
            }
            if (t.filtraPlaca()) {
                f.setPlaca(placa.getText());
            }
            if (t.filtraPeriodo()) {
                f.setDataInicio(inicio.getData("Data inicial"));
                f.setDataFim(fim.getData("Data final"));
            }
            if (t.filtraStatus() && status.getSelectedItem() instanceof StatusOrdem) {
                f.setStatus((StatusOrdem) status.getSelectedItem());
            }
            return ctx.relatorios().gerar(t, f);
        }).ifPresent(this::exibir);
    }

    private void exibir(Relatorio r) {
        atual = r;
        modelo.setDataVector(r.getLinhas().toArray(new Object[0][]), r.getColunas().toArray());
        tabela.setAutoCreateRowSorter(true);
        resumo.setText(r.getTitulo() + " - " + r.getResumo());
        exportar.setEnabled(true);
    }

    private void exportar() {
        if (atual == null) {
            UiUtil.aviso(this, "Gere um relatório antes de exportar.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar relatório para CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivo CSV (*.csv)", "csv"));
        String nome = "relatorio_" + ((TipoRelatorio) tipo.getSelectedItem()).name().toLowerCase() + "_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        chooser.setSelectedFile(new File(nome));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = chooser.getSelectedFile();
        if (!arquivo.getName().toLowerCase().endsWith(".csv")) {
            arquivo = new File(arquivo.getParentFile(), arquivo.getName() + ".csv");
        }
        if (arquivo.exists() && !UiUtil.confirmar(this, "O arquivo " + arquivo.getName() + " já existe. Substituir?")) {
            return;
        }
        Path destino = arquivo.toPath();
        if (UiUtil.executar(this, () -> ctx.relatorios().exportarCsv(atual, destino))) {
            UiUtil.sucesso(this, "Relatório exportado para:\n" + destino.toAbsolutePath());
        }
    }
}
