package com.hmz.agressores_da_bola.config.openapi;

import com.hmz.agressores_da_bola.exception.ErroResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 400 de Bean Validation, parâmetro fora do tipo ou corpo ilegível — tudo
 * tratado em {@code GlobalExceptionHandler} com o mesmo {@code ErroResponse}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(responseCode = "400", description = "Dados inválidos: campo obrigatório ausente, "
        + "formato incorreto ou enum desconhecido",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErroResponse.class)))
public @interface RespostaDadosInvalidos {
}
