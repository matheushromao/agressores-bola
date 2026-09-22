package com.hmz.agressores_da_bola.application.ranking.dto;

import com.hmz.agressores_da_bola.application.estatistica.dto.PontuacaoAtributoResponse;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResumoResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Uma linha da classificação geral, do primeiro ao último colocado.
 *
 * @param posicao colocação já resolvendo empates: dois jogadores com a mesma
 *                pontuação dividem a posição e a seguinte é pulada (1, 2, 2, 4)
 */
@Schema(requiredProperties = {
        "posicao",
        "jogador",
        "jogos",
        "gols",
        "assistencias",
        "desarmes",
        "defesas",
        "defesasDificeis",
        "pontuacao",
        "mediaPorJogo",
        "detalhamento"
})
public record RankingResponse(
        int posicao,
        UsuarioResumoResponse jogador,
        long jogos,
        long gols,
        long assistencias,
        long desarmes,
        long defesas,
        long defesasDificeis,
        int pontuacao,
        double mediaPorJogo,
        List<PontuacaoAtributoResponse> detalhamento
) {
}
