package com.hmz.agressores_da_bola.dto;

/**
 * Uma linha da artilharia, do ranking de assistências, de desarmes, de
 * defesas ou de defesas difíceis — todas têm o mesmo formato.
 */
public record RankingAtributoResponse(
        int posicao,
        UsuarioResumoResponse jogador,
        long jogos,
        int total,
        int pontos
) {
}
