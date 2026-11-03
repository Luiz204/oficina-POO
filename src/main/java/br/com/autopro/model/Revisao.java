package br.com.autopro.model;

import java.time.LocalDate;

public class Revisao {

    private Integer id;
    private Integer veiculoId;
    private LocalDate dataRevisao;
    private int km;
    private String observacoes;
    private String placa;

    public Revisao() {
    }

    public Revisao(Integer veiculoId, LocalDate dataRevisao, int km, String observacoes) {
        this.veiculoId = veiculoId;
        this.dataRevisao = dataRevisao;
        this.km = km;
        this.observacoes = observacoes;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Integer veiculoId) { this.veiculoId = veiculoId; }
    public LocalDate getDataRevisao() { return dataRevisao; }
    public void setDataRevisao(LocalDate dataRevisao) { this.dataRevisao = dataRevisao; }
    public int getKm() { return km; }
    public void setKm(int km) { this.km = km; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
}
