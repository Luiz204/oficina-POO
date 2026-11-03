package br.com.autopro.ui;

import br.com.autopro.service.AgendamentoService;
import br.com.autopro.service.ClienteService;
import br.com.autopro.service.ConfiguracaoService;
import br.com.autopro.service.DashboardService;
import br.com.autopro.service.EstoqueService;
import br.com.autopro.service.OrdemServicoService;
import br.com.autopro.service.PagamentoService;
import br.com.autopro.service.RelatorioService;
import br.com.autopro.service.RevisaoService;
import br.com.autopro.service.VeiculoService;

import java.awt.Color;

/** Reúne os serviços da aplicação e o acesso à janela principal (navegação e notificações). */
public class ContextoApp {

    private final ConfiguracaoService configuracao = new ConfiguracaoService();
    private final EstoqueService estoque = new EstoqueService();
    private final ClienteService clientes = new ClienteService();
    private final VeiculoService veiculos = new VeiculoService();
    private final OrdemServicoService ordens = new OrdemServicoService(estoque, configuracao);
    private final PagamentoService pagamentos = new PagamentoService();
    private final RevisaoService revisoes = new RevisaoService();
    private final AgendamentoService agendamentos = new AgendamentoService(configuracao);
    private final DashboardService dashboard = new DashboardService();
    private final RelatorioService relatorios = new RelatorioService();

    private MainFrame janela;

    void setJanela(MainFrame janela) {
        this.janela = janela;
    }

    public MainFrame getJanela() {
        return janela;
    }

    public void navegar(String modulo) {
        if (janela != null) {
            janela.navegar(modulo);
        }
    }

    public void notificar(String titulo, String mensagem, Color cor) {
        if (janela != null) {
            janela.notificar(titulo, mensagem, cor);
        }
    }

    public ConfiguracaoService configuracao() { return configuracao; }
    public EstoqueService estoque() { return estoque; }
    public ClienteService clientes() { return clientes; }
    public VeiculoService veiculos() { return veiculos; }
    public OrdemServicoService ordens() { return ordens; }
    public PagamentoService pagamentos() { return pagamentos; }
    public RevisaoService revisoes() { return revisoes; }
    public AgendamentoService agendamentos() { return agendamentos; }
    public DashboardService dashboard() { return dashboard; }
    public RelatorioService relatorios() { return relatorios; }
}
