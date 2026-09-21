package com.hmz.agressores_da_bola.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Contrato único de erro da API: vale tanto para o que o "
        + "GlobalExceptionHandler trata quanto para os 401 e 403 barrados no filtro de segurança",
        requiredProperties = {"timestamp", "status", "erro", "mensagem"})
public record ErroResponse(

        @Schema(description = "Momento em que o erro ocorreu", example = "2026-09-21T19:30:00")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP", example = "404")
        int status,

        @Schema(description = "Categoria do erro", example = "Recurso não encontrado")
        String erro,

        @Schema(description = "Explicação legível", example = "Pelada 42 não encontrada")
        String mensagem,

        @Schema(description = "Preenchido só nos 400 de validação: campo para mensagem",
                example = "{\"nome\": \"O nome da pelada é obrigatório\"}")
        Map<String, String> campos

) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, null);
    }

    public static ErroResponse deCampos(int status, String erro, String mensagem, Map<String, String> campos) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, campos);
    }
}
