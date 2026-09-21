package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Súmula de um jogador. `atualizadaEm` só existe depois da primeira correção.",
        requiredProperties = {
        "id",
        "peladaId",
        "jogador",
        "posicaoJogada",
        "posicaoJogadaDescricao",
        "goleiro",
        "gols",
        "assistencias",
        "desarmes",
        "defesas",
        "defesasDificeis",
        "pontuacao",
        "detalhamento",
        "registradaEm"
})
public record EstatisticaResponse(
        Long id,
        Long peladaId,
        UsuarioResumoResponse jogador,
        Posicao posicaoJogada,
        String posicaoJogadaDescricao,
        boolean goleiro,
        int gols,
        int assistencias,
        int desarmes,
        int defesas,
        int defesasDificeis,
        int pontuacao,
        List<PontuacaoAtributoResponse> detalhamento,
        LocalDateTime registradaEm,
        LocalDateTime atualizadaEm
) {
}
