package br.com.autopro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimentacaoEstoque {

    private Integer id;
    private Integer pecaId;
    private String pecaCodigo;
    private String pecaNome;
    private TipoMovimentacao tipo;
    private int quantidade;
    private BigDecimal precoUnitario;
    private Integer ordemId;
    private String placa;
    private LocalDateTime dataHora;
    private String observacao;

    public MovimentacaoEstoque() {
    }

    public MovimentacaoEstoque(Integer pecaId, TipoMovimentacao tipo, int quantidade, BigDecimal precoUnitario,
                               Integer ordemId, String observacao) {
        this.pecaId = pecaId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.ordemId = ordemId;
        this.observacao = observacao;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPecaId() { return pecaId; }
    public void setPecaId(Integer pecaId) { this.pecaId = pecaId; }
    public String getPecaCodigo() { return pecaCodigo; }
    public void setPecaCodigo(String pecaCodigo) { this.pecaCodigo = pecaCodigo; }
    public String getPecaNome() { return pecaNome; }
    public void setPecaNome(String pecaNome) { this.pecaNome = pecaNome; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public Integer getOrdemId() { return ordemId; }
    public void setOrdemId(Integer ordemId) { this.ordemId = ordemId; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
