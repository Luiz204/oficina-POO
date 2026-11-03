package br.com.autopro.model;

import java.math.BigDecimal;

public class Peca {

    private Integer id;
    private String codigo;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario = BigDecimal.ZERO;
    private int quantidade;
    private int estoqueMinimo;

    public Peca() {
    }

    public boolean isAbaixoDoMinimo() {
        return quantidade < estoqueMinimo;
    }

    public boolean possuiQuantidade(int necessaria) {
        return quantidade >= necessaria;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(int estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    @Override
    public String toString() {
        return codigo + " - " + nome + " (disp.: " + quantidade + ")";
    }
}
