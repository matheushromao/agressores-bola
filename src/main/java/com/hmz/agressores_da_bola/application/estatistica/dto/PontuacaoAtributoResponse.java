package com.hmz.agressores_da_bola.application.estatistica.dto;

import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Quebra da pontuação por atributo, para o front conseguir mostrar de onde
 * vieram os pontos do jogador em vez de só o total.
 */
@Schema(requiredProperties = {
        "atributo",
        "descricao",
        "quantidade",
        "peso",
        "pontos"
})
public record PontuacaoAtributoResponse(
        AtributoPontuacao atributo,
        String descricao,
        int quantidade,
        int peso,
        int pontos
) {
}
