package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import jakarta.validation.constraints.NotNull;

public record StatusPeladaRequest(

        @NotNull(message = "O status da pelada é obrigatório")
        StatusPelada status

) {
}
