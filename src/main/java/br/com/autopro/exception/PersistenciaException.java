package br.com.autopro.exception;

/** Falha técnica no acesso ao banco de dados. A mensagem não contém credenciais. */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
