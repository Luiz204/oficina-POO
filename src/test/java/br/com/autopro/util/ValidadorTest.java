package br.com.autopro.util;

import br.com.autopro.exception.NegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorTest {

    @Test
    void padronizaTelefonesValidos() {
        assertEquals("(11) 98765-4321", Validador.telefone("11987654321"));
        assertEquals("(11) 98765-4321", Validador.telefone("(11) 98765-4321"));
        assertEquals("(11) 3456-7890", Validador.telefone("11 3456 7890"));
    }

    @Test
    void recusaTelefonesInvalidos() {
        assertThrows(NegocioException.class, () -> Validador.telefone("12345"));
        assertThrows(NegocioException.class, () -> Validador.telefone("11887654321"));
        assertThrows(NegocioException.class, () -> Validador.telefone(""));
    }

    @Test
    void validaEmailOpcional() {
        assertNull(Validador.emailOpcional("  "));
        assertEquals("ana@oficina.com.br", Validador.emailOpcional("Ana@Oficina.com.br"));
        assertThrows(NegocioException.class, () -> Validador.emailOpcional("ana@"));
        assertThrows(NegocioException.class, () -> Validador.emailOpcional("ana.oficina.com"));
    }

    @Test
    void aceitaPlacasAntigaEMercosul() {
        assertEquals("ABC1234", Validador.placa("abc-1234"));
        assertEquals("ABC1D23", Validador.placa(" abc1d23 "));
        assertThrows(NegocioException.class, () -> Validador.placa("AB12345"));
        assertThrows(NegocioException.class, () -> Validador.placa("ABCD123"));
    }

    @Test
    void validaAnoQuilometragemEValores() {
        assertThrows(NegocioException.class, () -> Validador.ano(1899));
        assertThrows(NegocioException.class, () -> Validador.ano(LocalDate.now().getYear() + 2));
        assertEquals(2020, Validador.ano(2020));
        assertThrows(NegocioException.class, () -> Validador.quilometragem(-1, "Km"));
        assertThrows(NegocioException.class, () -> Validador.quantidadeNaoNegativa(-1, "Qtd"));
        assertThrows(NegocioException.class, () -> Validador.quantidadePositiva(0, "Qtd"));
        assertThrows(NegocioException.class, () -> Validador.valorNaoNegativo(new BigDecimal("-0.01"), "Preço"));
        assertThrows(NegocioException.class, () -> Validador.dataNaoFutura(LocalDate.now().plusDays(1), "Data"));
    }
}
