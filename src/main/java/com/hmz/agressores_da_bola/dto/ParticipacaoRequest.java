package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada para incluir um jogador na pelada. O status é opcional: quando o
 * organizador convida alguém envia {@code CONVIDADO}, quando o próprio
 * jogador entra envia {@code CONFIRMADO} (padrão).
 */
@Schema(description = "Inclusão de um jogador na escalação")
public record ParticipacaoRequest(

        @Schema(description = "Id do jogador a escalar", example = "3")
        @NotNull(message = "O usuário é obrigatório")
        Long usuarioId,

        @Schema(description = "CONVIDADO quando o organizador convida, CONFIRMADO quando o "
                + "próprio jogador entra. Omitido, vale CONFIRMADO.")
        StatusParticipacao status

) {

    public StatusParticipacao statusOuPadrao() {
        return status == null ? StatusParticipacao.CONFIRMADO : status;
    }
}
