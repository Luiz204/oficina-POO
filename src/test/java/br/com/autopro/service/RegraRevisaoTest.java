package br.com.autopro.service;

import br.com.autopro.model.SituacaoRevisao;
import br.com.autopro.model.Veiculo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraRevisaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

    private static Veiculo veiculo(int kmAtual, LocalDate ultima, Integer kmUltima) {
        Veiculo v = new Veiculo();
        v.setKmAtual(kmAtual);
        v.setDataUltimaRevisao(ultima);
        v.setKmUltimaRevisao(kmUltima);
        return v;
    }

    @Test
    void semHistoricoDeveSerAvaliado() {
        SituacaoRevisao s = RegraRevisao.avaliar(veiculo(50_000, null, null), HOJE);
        assertEquals(SituacaoRevisao.SEM_HISTORICO, s);
        assertTrue(s.requerAtencao());
    }

    @Test
    void emDiaAntesDeDezMilKmESeisMeses() {
        SituacaoRevisao s = RegraRevisao.avaliar(veiculo(59_999, HOJE.minusMonths(6).plusDays(1), 50_000), HOJE);
        assertEquals(SituacaoRevisao.EM_DIA, s);
        assertFalse(s.requerAtencao());
    }

    @Test
    void venceAoAtingirDezMilKm() {
        assertEquals(SituacaoRevisao.VENCIDA_POR_KM,
                RegraRevisao.avaliar(veiculo(60_000, HOJE.minusMonths(1), 50_000), HOJE));
    }

    @Test
    void venceAoCompletarSeisMeses() {
        assertEquals(SituacaoRevisao.VENCIDA_POR_DATA,
                RegraRevisao.avaliar(veiculo(51_000, HOJE.minusMonths(6), 50_000), HOJE));
    }

    @Test
    void oQueOcorrerPrimeiro() {
        assertEquals(SituacaoRevisao.VENCIDA_POR_KM_E_DATA,
                RegraRevisao.avaliar(veiculo(70_000, HOJE.minusYears(1), 50_000), HOJE));
    }

    @Test
    void calculaProximaRevisao() {
        Veiculo v = veiculo(55_000, LocalDate.of(2026, 1, 31), 50_000);
        assertEquals(LocalDate.of(2026, 7, 31), RegraRevisao.proximaPorData(v));
        assertEquals(60_000, RegraRevisao.proximaPorKm(v));
    }
}
