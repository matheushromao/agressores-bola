package com.hmz.agressores_da_bola.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Token de acesso devolvido pelo login. O cliente envia
 * {@code Authorization: <tipo> <token>} em cada request até {@code expiraEm}.
 */
@Schema(requiredProperties = {
        "token",
        "tipo",
        "expiraEm"
})
public record TokenResponse(
        String token,
        String tipo,
        Instant expiraEm
) {
}
