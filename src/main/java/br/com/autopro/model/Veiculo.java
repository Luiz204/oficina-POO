package br.com.autopro.model;

import java.time.LocalDate;

public class Veiculo {

    private Integer id;
    private Integer clienteId;
    private String modelo;
    private int ano;
    private String placa;
    private int kmAtual;
    private LocalDate dataUltimaRevisao;
    private Integer kmUltimaRevisao;

    // Dados do proprietário carregados via JOIN (somente exibição)
    private String clienteNome;
    private CategoriaCliente clienteCategoria;

    public Veiculo() {
    }

    public boolean possuiHistoricoRevisao() {
        return dataUltimaRevisao != null && kmUltimaRevisao != null;
    }

    public boolean isProprietarioVip() {
        return clienteCategoria == CategoriaCliente.VIP;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public int getKmAtual() { return kmAtual; }
    public void setKmAtual(int kmAtual) { this.kmAtual = kmAtual; }
    public LocalDate getDataUltimaRevisao() { return dataUltimaRevisao; }
    public void setDataUltimaRevisao(LocalDate dataUltimaRevisao) { this.dataUltimaRevisao = dataUltimaRevisao; }
    public Integer getKmUltimaRevisao() { return kmUltimaRevisao; }
    public void setKmUltimaRevisao(Integer kmUltimaRevisao) { this.kmUltimaRevisao = kmUltimaRevisao; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public CategoriaCliente getClienteCategoria() { return clienteCategoria; }
    public void setClienteCategoria(CategoriaCliente clienteCategoria) { this.clienteCategoria = clienteCategoria; }

    @Override
    public String toString() {
        return placa + " - " + modelo + (clienteNome != null ? " (" + clienteNome + ")" : "");
    }
}
