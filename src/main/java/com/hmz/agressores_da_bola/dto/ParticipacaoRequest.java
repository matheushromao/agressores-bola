package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada para incluir um jogador na pelada. O status é opcional: quando o
 * organizador convida alguém envia {@code CONVIDADO}, quando o próprio
 * jogador entra envia {@code CONFIRMADO} (padrão).
 */
public record ParticipacaoRequest(

        @NotNull(message = "O usuário é obrigatório")
        Long usuarioId,

        StatusParticipacao status

) {

    public StatusParticipacao statusOuPadrao() {
        return status == null ? StatusParticipacao.CONFIRMADO : status;
    }
}
