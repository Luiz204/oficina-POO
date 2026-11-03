package br.com.autopro.exception;

import java.util.Collections;
import java.util.List;

/** Lançada quando uma ou mais peças não possuem quantidade suficiente em estoque. */
public class EstoqueInsuficienteException extends NegocioException {

    private static final long serialVersionUID = 1L;

    private final List<String> pendencias;

    public EstoqueInsuficienteException(List<String> pendencias) {
        super("Estoque insuficiente para iniciar o serviço:\n• " + String.join("\n• ", pendencias));
        this.pendencias = Collections.unmodifiableList(pendencias);
    }

    public List<String> getPendencias() {
        return pendencias;
    }
}
