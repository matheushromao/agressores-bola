package com.hmz.agressores_da_bola.web.openapi;

import com.hmz.agressores_da_bola.web.error.ErroResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 404 de {@code RecursoNaoEncontradoException}. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(responseCode = "404", description = "Recurso não encontrado",
        content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErroResponse.class)))
public @interface RespostaNaoEncontrado {
}
