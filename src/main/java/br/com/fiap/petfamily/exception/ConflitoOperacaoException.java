package br.com.fiap.petfamily.exception;

public class ConflitoOperacaoException extends RuntimeException {
    public ConflitoOperacaoException(String message) {
        super(message);
    }
}
