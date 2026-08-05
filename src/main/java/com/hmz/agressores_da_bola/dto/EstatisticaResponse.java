package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;

import java.time.LocalDateTime;
import java.util.List;

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
