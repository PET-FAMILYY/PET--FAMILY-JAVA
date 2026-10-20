package br.com.fiap.petfamily.exception;

public class TentativasExcedidasException extends RuntimeException {
    public TentativasExcedidasException() {
        super("Muitas tentativas de login. Aguarde alguns minutos e tente novamente.");
    }
}
