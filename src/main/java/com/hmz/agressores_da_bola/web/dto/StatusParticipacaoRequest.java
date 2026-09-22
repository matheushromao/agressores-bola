package com.hmz.agressores_da_bola.web.dto;

import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nova situação da participação de um jogador")
public record StatusParticipacaoRequest(

        @Schema(description = "Só CONFIRMADO ocupa vaga. Sair de CONFIRMADO libera a vaga e "
                + "promove o primeiro da lista de espera.")
        @NotNull(message = "O status da participação é obrigatório")
        StatusParticipacao status

) {
}
