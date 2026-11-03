package br.com.autopro.util;

import br.com.autopro.exception.NegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatadorTest {

    @Test
    void formataMoedaEmReais() {
        assertEquals("R$ 1.234,50", Formatador.moeda(new BigDecimal("1234.5")));
        assertEquals("R$ 0,00", Formatador.moeda(BigDecimal.ZERO));
    }

    @Test
    void interpretaValoresNoPadraoBrasileiro() {
        assertEquals(new BigDecimal("1234.56"), Formatador.parseMoeda("1.234,56", "Preço"));
        assertEquals(new BigDecimal("1234.56"), Formatador.parseMoeda("R$ 1234,56", "Preço"));
        assertEquals(new BigDecimal("10.50"), Formatador.parseMoeda("10.5", "Preço"));
        assertThrows(NegocioException.class, () -> Formatador.parseMoeda("abc", "Preço"));
    }

    @Test
    void formataEInterpretaDatas() {
        assertEquals("05/03/2026", Formatador.data(LocalDate.of(2026, 3, 5)));
        assertEquals("05/03/2026 14:30", Formatador.dataHora(LocalDateTime.of(2026, 3, 5, 14, 30)));
        assertEquals(LocalDate.of(2026, 2, 28), Formatador.parseData("28/02/2026", "Data"));
        assertNull(Formatador.parseData("__/__/____", "Data"));
        assertThrows(NegocioException.class, () -> Formatador.parseData("30/02/2026", "Data"));
        assertThrows(NegocioException.class, () -> Formatador.parseDataHora("01/01/2026 25:00", "Data"));
    }
}
