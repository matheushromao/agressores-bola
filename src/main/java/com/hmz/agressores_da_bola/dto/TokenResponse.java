package com.hmz.agressores_da_bola.dto;

import java.time.Instant;

/**
 * Token de acesso devolvido pelo login. O cliente envia
 * {@code Authorization: <tipo> <token>} em cada request até {@code expiraEm}.
 */
public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm
) {
}
