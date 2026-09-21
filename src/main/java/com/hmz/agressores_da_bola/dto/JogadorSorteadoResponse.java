package com.hmz.agressores_da_bola.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {
        "jogador",
        "goleiro"
})
public record JogadorSorteadoResponse(
        UsuarioResumoResponse jogador,
        boolean goleiro
) {
}
