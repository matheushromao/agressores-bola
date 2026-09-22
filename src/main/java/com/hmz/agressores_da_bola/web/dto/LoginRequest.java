package com.hmz.agressores_da_bola.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(

        @Schema(description = "E-mail do cadastro", example = "matheus@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        String email,

        @Schema(description = "Senha do cadastro", example = "senhaSegura123")
        @NotBlank(message = "A senha é obrigatória")
        String senha

) {
}
