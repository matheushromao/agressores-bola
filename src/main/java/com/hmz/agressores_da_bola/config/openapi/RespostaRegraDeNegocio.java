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
 * 409 de {@code RegraDeNegocioException} — pelada lotada, status incompatível,
 * jogador já escalado, violação de unicidade.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(responseCode = "409", description = "A operação viola uma regra de negócio",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErroResponse.class)))
public @interface RespostaRegraDeNegocio {
}
