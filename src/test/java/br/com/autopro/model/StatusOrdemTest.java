package br.com.autopro.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusOrdemTest {

    @Test
    void permiteSomenteOFluxoSequencial() {
        assertTrue(StatusOrdem.AGUARDANDO_PECAS.podeTransicionarPara(StatusOrdem.EM_SERVICO));
        assertTrue(StatusOrdem.EM_SERVICO.podeTransicionarPara(StatusOrdem.PRONTO_PARA_ENTREGA));
        assertTrue(StatusOrdem.PRONTO_PARA_ENTREGA.podeTransicionarPara(StatusOrdem.ENTREGUE));
    }

    @Test
    void impedeTransicoesInvalidas() {
        assertFalse(StatusOrdem.AGUARDANDO_PECAS.podeTransicionarPara(StatusOrdem.PRONTO_PARA_ENTREGA));
        assertFalse(StatusOrdem.AGUARDANDO_PECAS.podeTransicionarPara(StatusOrdem.ENTREGUE));
        assertFalse(StatusOrdem.EM_SERVICO.podeTransicionarPara(StatusOrdem.ENTREGUE));
        assertFalse(StatusOrdem.EM_SERVICO.podeTransicionarPara(StatusOrdem.AGUARDANDO_PECAS));
        assertFalse(StatusOrdem.PRONTO_PARA_ENTREGA.podeTransicionarPara(StatusOrdem.EM_SERVICO));
        for (StatusOrdem s : StatusOrdem.values()) {
            assertFalse(StatusOrdem.ENTREGUE.podeTransicionarPara(s), "ENTREGUE é estado final");
            assertFalse(s.podeTransicionarPara(s), "não transiciona para si mesmo");
        }
    }

    @Test
    void itensSoPodemSerAlteradosAntesDoServico() {
        assertTrue(StatusOrdem.AGUARDANDO_PECAS.permiteAlterarItens());
        assertFalse(StatusOrdem.EM_SERVICO.permiteAlterarItens());
        assertTrue(StatusOrdem.EM_SERVICO.permiteAlterarServico());
        assertFalse(StatusOrdem.PRONTO_PARA_ENTREGA.permiteAlterarServico());
    }
}
