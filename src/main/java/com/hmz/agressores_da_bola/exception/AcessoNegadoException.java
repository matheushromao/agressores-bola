package com.hmz.agressores_da_bola.exception;

/**
 * O usuário está autenticado, mas não é dono do recurso que tentou alterar
 * (por exemplo, editar a pelada de outro organizador).
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
