package br.com.autopro.model;

import java.time.LocalDateTime;

public class Agendamento {

    private Integer id;
    private Integer veiculoId;
    private LocalDateTime dataHora;
    private TipoAgendamento tipo = TipoAgendamento.SERVICO;
    private String descricao;
    private Integer duracaoMinutos;
    private StatusAgendamento status = StatusAgendamento.AGENDADO;
    private Integer ordemId;

    // Dados de exibição
    private String placa;
    private String modeloVeiculo;
    private Integer clienteId;
    private String clienteNome;
    private String clienteTelefone;
    private CategoriaCliente clienteCategoria;

    public Agendamento() {
    }

    public boolean isClienteVip() {
        return clienteCategoria == CategoriaCliente.VIP;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Integer veiculoId) { this.veiculoId = veiculoId; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public TipoAgendamento getTipo() { return tipo; }
    public void setTipo(TipoAgendamento tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public StatusAgendamento getStatus() { return status; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public Integer getOrdemId() { return ordemId; }
    public void setOrdemId(Integer ordemId) { this.ordemId = ordemId; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getModeloVeiculo() { return modeloVeiculo; }
    public void setModeloVeiculo(String modeloVeiculo) { this.modeloVeiculo = modeloVeiculo; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public String getClienteTelefone() { return clienteTelefone; }
    public void setClienteTelefone(String clienteTelefone) { this.clienteTelefone = clienteTelefone; }
    public CategoriaCliente getClienteCategoria() { return clienteCategoria; }
    public void setClienteCategoria(CategoriaCliente clienteCategoria) { this.clienteCategoria = clienteCategoria; }
}
