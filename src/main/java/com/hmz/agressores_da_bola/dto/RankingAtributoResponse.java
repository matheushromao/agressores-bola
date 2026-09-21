package com.hmz.agressores_da_bola.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Uma linha da artilharia, do ranking de assistências, de desarmes, de
 * defesas ou de defesas difíceis — todas têm o mesmo formato.
 */
@Schema(requiredProperties = {
        "posicao",
        "jogador",
        "jogos",
        "total",
        "pontos"
})
public record RankingAtributoResponse(
        int posicao,
        UsuarioResumoResponse jogador,
        long jogos,
        int total,
        int pontos
) {
}
