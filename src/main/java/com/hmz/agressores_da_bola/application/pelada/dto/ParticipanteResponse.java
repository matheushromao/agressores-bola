package com.hmz.agressores_da_bola.application.pelada.dto;

import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResumoResponse;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(requiredProperties = {
        "participacaoId",
        "usuario",
        "status",
        "statusDescricao",
        "dataInscricao"
})
public record ParticipanteResponse(
        Long participacaoId,
        UsuarioResumoResponse usuario,
        StatusParticipacao status,
        String statusDescricao,
        LocalDateTime dataInscricao
) {
}
