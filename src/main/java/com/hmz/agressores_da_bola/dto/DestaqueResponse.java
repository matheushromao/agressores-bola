package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;

import java.util.List;

/**
 * Agrupa, em uma única resposta, o ranking de cada atributo — quem mais fez
 * gols, quem mais deu assistência, quem mais fez defesa difícil — para a tela
 * de destaques não precisar de uma chamada por atributo.
 */
public record DestaqueResponse(
        AtributoPontuacao atributo,
        String descricao,
        int peso,
        List<RankingAtributoResponse> ranking
) {
}
