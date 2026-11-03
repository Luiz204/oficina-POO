package br.com.autopro.service;

import br.com.autopro.dao.ConfiguracaoDAO;
import br.com.autopro.exception.NegocioException;
import br.com.autopro.util.Transacao;

import java.util.Optional;

/** Parâmetros configuráveis pelo operador, armazenados na tabela configuracoes. */
public class ConfiguracaoService {

    public static final String DURACAO_REVISAO_RAPIDA = "revisao_rapida_duracao_minutos";

    private final ConfiguracaoDAO dao = new ConfiguracaoDAO();

    /** Duração máxima (min) do atendimento/revisão rápida. Vazio enquanto o operador não configurar. */
    public Optional<Integer> duracaoRevisaoRapida() {
        Optional<String> valor = Transacao.consultar(c -> dao.obter(c, DURACAO_REVISAO_RAPIDA));
        try {
            return valor.filter(v -> !v.isBlank()).map(String::trim).map(Integer::valueOf);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Define a duração; null remove a configuração. */
    public void definirDuracaoRevisaoRapida(Integer minutos) {
        if (minutos != null && (minutos <= 0 || minutos > 24 * 60)) {
            throw new NegocioException("A duração deve estar entre 1 e 1440 minutos.");
        }
        Transacao.executar(c -> {
            dao.salvar(c, DURACAO_REVISAO_RAPIDA, minutos == null ? null : minutos.toString());
        });
    }
}
