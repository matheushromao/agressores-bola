package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;

import java.time.LocalDateTime;

public record ParticipanteResponse(
        Long participacaoId,
        UsuarioResumoResponse usuario,
        StatusParticipacao status,
        String statusDescricao,
        LocalDateTime dataInscricao
) {
}
