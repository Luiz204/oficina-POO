package br.com.autopro.model;

import java.time.LocalDateTime;

public class HistoricoStatus {

    private Integer id;
    private Integer ordemId;
    private StatusOrdem statusAnterior;
    private StatusOrdem statusNovo;
    private LocalDateTime dataHora;
    private String observacao;

    public HistoricoStatus() {
    }

    public HistoricoStatus(Integer ordemId, StatusOrdem statusAnterior, StatusOrdem statusNovo, String observacao) {
        this.ordemId = ordemId;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.observacao = observacao;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOrdemId() { return ordemId; }
    public void setOrdemId(Integer ordemId) { this.ordemId = ordemId; }
    public StatusOrdem getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(StatusOrdem statusAnterior) { this.statusAnterior = statusAnterior; }
    public StatusOrdem getStatusNovo() { return statusNovo; }
    public void setStatusNovo(StatusOrdem statusNovo) { this.statusNovo = statusNovo; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
