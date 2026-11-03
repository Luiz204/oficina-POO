package br.com.autopro.model;

import java.math.BigDecimal;

/** Objeto imutável com o resultado do cálculo financeiro de uma ordem. */
public final class ValoresOrdem {

    private final BigDecimal valorPecas;
    private final BigDecimal valorMaoObra;
    private final BigDecimal desconto;
    private final BigDecimal valorTotal;

    public ValoresOrdem(BigDecimal valorPecas, BigDecimal valorMaoObra, BigDecimal desconto, BigDecimal valorTotal) {
        this.valorPecas = valorPecas;
        this.valorMaoObra = valorMaoObra;
        this.desconto = desconto;
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorPecas() { return valorPecas; }
    public BigDecimal getValorMaoObra() { return valorMaoObra; }
    public BigDecimal getDesconto() { return desconto; }
    public BigDecimal getValorTotal() { return valorTotal; }
}
