package com.hmz.agressores_da_bola.exception;

/**
 * Login recusado. A mensagem é a mesma para e-mail inexistente e senha
 * errada, para não revelar quais e-mails estão cadastrados.
 */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
