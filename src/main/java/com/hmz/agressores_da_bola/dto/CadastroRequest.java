package com.hmz.agressores_da_bola.dto;

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
public record CadastroRequest(

        @NotNull(message = "Os dados do jogador são obrigatórios")
        @Valid
        UsuarioRequest usuario,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha

) {
}
