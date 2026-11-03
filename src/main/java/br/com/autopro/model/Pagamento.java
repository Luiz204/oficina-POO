package br.com.autopro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pagamento {

    private Integer id;
    private Integer ordemId;
    private BigDecimal valor;
    private FormaPagamento forma;
    private LocalDateTime dataHora;
    private String situacao = "CONFIRMADO";
    private String observacao;

    // Dados de exibição
    private String clienteNome;
    private String placa;

    public Pagamento() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOrdemId() { return ordemId; }
    public void setOrdemId(Integer ordemId) { this.ordemId = ordemId; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public FormaPagamento getForma() { return forma; }
    public void setForma(FormaPagamento forma) { this.forma = forma; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getSituacao() { return situacao; }
    public void setSituacao(String situacao) { this.situacao = situacao; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
}
