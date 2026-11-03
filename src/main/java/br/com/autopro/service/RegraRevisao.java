package br.com.autopro.service;

import br.com.autopro.model.SituacaoRevisao;
import br.com.autopro.model.Veiculo;

import java.time.LocalDate;

/**
 * Regra de revisão periódica: recomendada a cada 10.000 km desde a última revisão
 * ou a cada 6 meses, o que ocorrer primeiro. Veículos sem histórico devem ser avaliados.
 */
public final class RegraRevisao {

    public static final int INTERVALO_KM = 10_000;
    public static final int INTERVALO_MESES = 6;

    private RegraRevisao() {
    }

    public static SituacaoRevisao avaliar(Veiculo v, LocalDate hoje) {
        if (!v.possuiHistoricoRevisao()) {
            return SituacaoRevisao.SEM_HISTORICO;
        }
        boolean porKm = v.getKmAtual() - v.getKmUltimaRevisao() >= INTERVALO_KM;
        boolean porData = !hoje.isBefore(proximaPorData(v));
        if (porKm && porData) {
            return SituacaoRevisao.VENCIDA_POR_KM_E_DATA;
        }
        if (porKm) {
            return SituacaoRevisao.VENCIDA_POR_KM;
        }
        if (porData) {
            return SituacaoRevisao.VENCIDA_POR_DATA;
        }
        return SituacaoRevisao.EM_DIA;
    }

    /** Data limite da próxima revisão, ou null se o veículo não tiver histórico. */
    public static LocalDate proximaPorData(Veiculo v) {
        return v.getDataUltimaRevisao() == null ? null : v.getDataUltimaRevisao().plusMonths(INTERVALO_MESES);
    }

    /** Quilometragem limite da próxima revisão, ou null se o veículo não tiver histórico. */
    public static Integer proximaPorKm(Veiculo v) {
        return v.getKmUltimaRevisao() == null ? null : v.getKmUltimaRevisao() + INTERVALO_KM;
    }
}
