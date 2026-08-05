package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;

/**
 * Quebra da pontuação por atributo, para o front conseguir mostrar de onde
 * vieram os pontos do jogador em vez de só o total.
 */
public record PontuacaoAtributoResponse(
        AtributoPontuacao atributo,
        String descricao,
        int quantidade,
        int peso,
        int pontos
) {
}
