package com.hmz.agressores_da_bola.application.ranking.dto;

import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Agrupa, em uma única resposta, o ranking de cada atributo — quem mais fez
 * gols, quem mais deu assistência, quem mais fez defesa difícil — para a tela
 * de destaques não precisar de uma chamada por atributo.
 */
@Schema(requiredProperties = {
        "atributo",
        "descricao",
        "peso",
        "ranking"
})
public record DestaqueResponse(
        AtributoPontuacao atributo,
        String descricao,
        int peso,
        List<RankingAtributoResponse> ranking
) {
}
