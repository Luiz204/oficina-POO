package br.com.autopro.exception;

/**
 * Violação de regra de negócio ou de validação. A mensagem é sempre
 * compreensível para o usuário final e pode ser exibida diretamente na tela.
 */
public class NegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
