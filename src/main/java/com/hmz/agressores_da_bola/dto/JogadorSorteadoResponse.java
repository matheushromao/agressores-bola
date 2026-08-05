package com.hmz.agressores_da_bola.dto;

public record JogadorSorteadoResponse(
        UsuarioResumoResponse jogador,
        boolean goleiro
) {
}
