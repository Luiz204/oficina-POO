package br.com.autopro.model;

import java.math.BigDecimal;

/**
 * Peça necessária em uma ordem de serviço.
 * O preço unitário é copiado da peça no momento da inclusão e preservado no histórico.
 */
public class ItemOrdem {

    private Integer id;
    private Integer ordemId;
    private Integer pecaId;
    private String pecaCodigo;
    private String pecaNome;
    private int quantidade;
    private BigDecimal precoUnitario = BigDecimal.ZERO;

    public ItemOrdem() {
    }

    public ItemOrdem(Integer pecaId, String pecaCodigo, String pecaNome, int quantidade, BigDecimal precoUnitario) {
        this.pecaId = pecaId;
        this.pecaCodigo = pecaCodigo;
        this.pecaNome = pecaNome;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOrdemId() { return ordemId; }
    public void setOrdemId(Integer ordemId) { this.ordemId = ordemId; }
    public Integer getPecaId() { return pecaId; }
    public void setPecaId(Integer pecaId) { this.pecaId = pecaId; }
    public String getPecaCodigo() { return pecaCodigo; }
    public void setPecaCodigo(String pecaCodigo) { this.pecaCodigo = pecaCodigo; }
    public String getPecaNome() { return pecaNome; }
    public void setPecaNome(String pecaNome) { this.pecaNome = pecaNome; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
}
