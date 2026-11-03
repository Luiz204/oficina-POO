package br.com.autopro.model;

/** Números exibidos no dashboard, todos obtidos do banco de dados. */
public class Indicadores {

    private int totalClientes;
    private int totalVeiculos;
    private int ordensAguardandoPecas;
    private int ordensEmServico;
    private int veiculosProntos;
    private int ordensAguardandoPagamento;
    private int revisoesVencidas;
    private int pecasAbaixoMinimo;
    private int proximosAgendamentos;

    public int getTotalClientes() { return totalClientes; }
    public void setTotalClientes(int v) { this.totalClientes = v; }
    public int getTotalVeiculos() { return totalVeiculos; }
    public void setTotalVeiculos(int v) { this.totalVeiculos = v; }
    public int getOrdensAguardandoPecas() { return ordensAguardandoPecas; }
    public void setOrdensAguardandoPecas(int v) { this.ordensAguardandoPecas = v; }
    public int getOrdensEmServico() { return ordensEmServico; }
    public void setOrdensEmServico(int v) { this.ordensEmServico = v; }
    public int getVeiculosProntos() { return veiculosProntos; }
    public void setVeiculosProntos(int v) { this.veiculosProntos = v; }
    public int getOrdensAguardandoPagamento() { return ordensAguardandoPagamento; }
    public void setOrdensAguardandoPagamento(int v) { this.ordensAguardandoPagamento = v; }
    public int getRevisoesVencidas() { return revisoesVencidas; }
    public void setRevisoesVencidas(int v) { this.revisoesVencidas = v; }
    public int getPecasAbaixoMinimo() { return pecasAbaixoMinimo; }
    public void setPecasAbaixoMinimo(int v) { this.pecasAbaixoMinimo = v; }
    public int getProximosAgendamentos() { return proximosAgendamentos; }
    public void setProximosAgendamentos(int v) { this.proximosAgendamentos = v; }
}
