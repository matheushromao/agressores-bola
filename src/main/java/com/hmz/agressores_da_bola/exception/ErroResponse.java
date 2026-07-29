package com.hmz.agressores_da_bola.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        Map<String, String> campos
) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, null);
    }

    public static ErroResponse deCampos(int status, String erro, String mensagem, Map<String, String> campos) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, campos);
    }
}
