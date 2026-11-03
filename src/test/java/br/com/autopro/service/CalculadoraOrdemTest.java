package br.com.autopro.service;

import br.com.autopro.exception.NegocioException;
import br.com.autopro.model.CategoriaCliente;
import br.com.autopro.model.ItemOrdem;
import br.com.autopro.model.ValoresOrdem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadoraOrdemTest {

    private static ItemOrdem item(int qtd, String preco) {
        return new ItemOrdem(1, "P1", "Peça", qtd, new BigDecimal(preco));
    }

    @Test
    void maoDeObraCustaCentoEVinteReaisPorHora() {
        assertEquals(new BigDecimal("120.00"), CalculadoraOrdem.calcularMaoObra(BigDecimal.ONE));
        assertEquals(new BigDecimal("300.00"), CalculadoraOrdem.calcularMaoObra(new BigDecimal("2.5")));
        assertEquals(new BigDecimal("0.00"), CalculadoraOrdem.calcularMaoObra(BigDecimal.ZERO));
    }

    @Test
    void valorDasPecasEhSomaDeQuantidadeVezesPreco() {
        List<ItemOrdem> itens = List.of(item(4, "42.50"), item(1, "35.90"));
        assertEquals(new BigDecimal("205.90"), CalculadoraOrdem.calcularValorPecas(itens));
    }

    @Test
    void clienteComumNaoTemDesconto() {
        ValoresOrdem v = CalculadoraOrdem.calcular(List.of(item(2, "100.00")), new BigDecimal("2"),
                CategoriaCliente.COMUM);
        assertEquals(new BigDecimal("200.00"), v.getValorPecas());
        assertEquals(new BigDecimal("240.00"), v.getValorMaoObra());
        assertEquals(new BigDecimal("0.00"), v.getDesconto());
        assertEquals(new BigDecimal("440.00"), v.getValorTotal());
    }

    @Test
    void vipTemDezPorCentoDeDescontoSomenteNaMaoDeObra() {
        ValoresOrdem v = CalculadoraOrdem.calcular(List.of(item(2, "100.00")), new BigDecimal("2"),
                CategoriaCliente.VIP);
        assertEquals(new BigDecimal("200.00"), v.getValorPecas(), "peças sem desconto");
        assertEquals(new BigDecimal("24.00"), v.getDesconto(), "10% de 240,00");
        assertEquals(new BigDecimal("416.00"), v.getValorTotal());
    }

    @Test
    void descontoEhArredondadoEmDuasCasas() {
        // 1,33 h x 120 = 159,60 -> 10% = 15,96
        ValoresOrdem v = CalculadoraOrdem.calcular(List.of(), new BigDecimal("1.33"), CategoriaCliente.VIP);
        assertEquals(new BigDecimal("159.60"), v.getValorMaoObra());
        assertEquals(new BigDecimal("15.96"), v.getDesconto());
        assertEquals(new BigDecimal("143.64"), v.getValorTotal());
    }

    @Test
    void recusaHorasNegativasEQuantidadeZero() {
        assertThrows(NegocioException.class, () -> CalculadoraOrdem.calcularMaoObra(new BigDecimal("-1")));
        assertThrows(NegocioException.class, () -> CalculadoraOrdem.calcularValorPecas(List.of(item(0, "10"))));
    }
}
