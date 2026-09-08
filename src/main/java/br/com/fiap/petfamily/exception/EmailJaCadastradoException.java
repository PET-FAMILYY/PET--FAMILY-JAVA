package br.com.fiap.petfamily.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("Já existe uma conta cadastrada com o e-mail: " + email);
    }
}
