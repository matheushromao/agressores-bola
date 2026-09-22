package com.hmz.agressores_da_bola.application.sorteio.dto;

import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResumoResponse;
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
