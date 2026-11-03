package br.com.autopro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdemServico {

    private Integer id;
    private Integer veiculoId;
    private Integer clienteId;
    private String descricao;
    private BigDecimal horasMaoObra = BigDecimal.ZERO;
    private LocalDateTime dataAbertura;
    private LocalDate dataAgendada;
    private StatusOrdem status = StatusOrdem.AGUARDANDO_PECAS;
    private BigDecimal valorPecas = BigDecimal.ZERO;
    private BigDecimal valorMaoObra = BigDecimal.ZERO;
    private BigDecimal desconto = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private SituacaoPagamento situacaoPagamento = SituacaoPagamento.PENDENTE;
    private boolean estoqueBaixado;
    private LocalDateTime dataConclusao;
    private LocalDateTime dataEntrega;
    private List<ItemOrdem> itens = new ArrayList<>();

    // Dados de exibição carregados via JOIN
    private String placa;
    private String modeloVeiculo;
    private String clienteNome;
    private CategoriaCliente clienteCategoria;

    public OrdemServico() {
    }

    public boolean isClienteVip() {
        return clienteCategoria == CategoriaCliente.VIP;
    }

    public boolean isPaga() {
        return situacaoPagamento == SituacaoPagamento.PAGO;
    }

    /** Aplica os valores calculados pela calculadora de ordens. */
    public void aplicarValores(ValoresOrdem valores) {
        this.valorPecas = valores.getValorPecas();
        this.valorMaoObra = valores.getValorMaoObra();
        this.desconto = valores.getDesconto();
        this.valorTotal = valores.getValorTotal();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Integer veiculoId) { this.veiculoId = veiculoId; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getHorasMaoObra() { return horasMaoObra; }
    public void setHorasMaoObra(BigDecimal horasMaoObra) { this.horasMaoObra = horasMaoObra; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }
    public LocalDate getDataAgendada() { return dataAgendada; }
    public void setDataAgendada(LocalDate dataAgendada) { this.dataAgendada = dataAgendada; }
    public StatusOrdem getStatus() { return status; }
    public void setStatus(StatusOrdem status) { this.status = status; }
    public BigDecimal getValorPecas() { return valorPecas; }
    public void setValorPecas(BigDecimal valorPecas) { this.valorPecas = valorPecas; }
    public BigDecimal getValorMaoObra() { return valorMaoObra; }
    public void setValorMaoObra(BigDecimal valorMaoObra) { this.valorMaoObra = valorMaoObra; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public SituacaoPagamento getSituacaoPagamento() { return situacaoPagamento; }
    public void setSituacaoPagamento(SituacaoPagamento situacaoPagamento) { this.situacaoPagamento = situacaoPagamento; }
    public boolean isEstoqueBaixado() { return estoqueBaixado; }
    public void setEstoqueBaixado(boolean estoqueBaixado) { this.estoqueBaixado = estoqueBaixado; }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDateTime dataConclusao) { this.dataConclusao = dataConclusao; }
    public LocalDateTime getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }
    public List<ItemOrdem> getItens() { return itens; }
    public void setItens(List<ItemOrdem> itens) { this.itens = itens != null ? itens : new ArrayList<>(); }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getModeloVeiculo() { return modeloVeiculo; }
    public void setModeloVeiculo(String modeloVeiculo) { this.modeloVeiculo = modeloVeiculo; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public CategoriaCliente getClienteCategoria() { return clienteCategoria; }
    public void setClienteCategoria(CategoriaCliente clienteCategoria) { this.clienteCategoria = clienteCategoria; }
}
