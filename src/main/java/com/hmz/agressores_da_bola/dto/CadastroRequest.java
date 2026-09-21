package com.hmz.agressores_da_bola.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de jogador. Os dados do perfil reaproveitam o {@link UsuarioRequest}
 * (e todas as suas validações); a senha fica ao lado porque só existe no
 * cadastro — a edição de perfil não mexe nela.
 *
 * <p>O teto de 72 caracteres é o limite do BCrypt: o que passar disso seria
 * ignorado silenciosamente no hash.</p>
 */
@Schema(description = "Perfil do jogador mais a senha de acesso")
public record CadastroRequest(

        @Schema(description = "Dados do perfil, idênticos aos da edição")
        @NotNull(message = "Os dados do jogador são obrigatórios")
        @Valid
        UsuarioRequest usuario,

        @Schema(description = "Senha de 8 a 72 caracteres; o teto é o limite do BCrypt",
                example = "senhaSegura123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha

) {
}
