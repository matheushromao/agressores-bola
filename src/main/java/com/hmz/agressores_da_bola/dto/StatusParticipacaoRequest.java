package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import jakarta.validation.constraints.NotNull;

public record StatusParticipacaoRequest(

        @NotNull(message = "O status da participação é obrigatório")
        StatusParticipacao status

) {
}
