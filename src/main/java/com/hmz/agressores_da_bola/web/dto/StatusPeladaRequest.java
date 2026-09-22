package com.hmz.agressores_da_bola.web.dto;

import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nova situação da pelada")
public record StatusPeladaRequest(

        @Schema(description = "Situação de destino, respeitando as transições permitidas")
        @NotNull(message = "O status da pelada é obrigatório")
        StatusPelada status

) {
}
