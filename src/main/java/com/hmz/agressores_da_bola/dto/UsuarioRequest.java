package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(

        @NotBlank(message = "O nome completo é obrigatório")
        @Size(min = 3, max = 120, message = "O nome completo deve ter entre 3 e 120 caracteres")
        String nomeCompleto,

        @NotBlank(message = "O nickname é obrigatório")
        @Size(min = 3, max = 30, message = "O nickname deve ter entre 3 e 30 caracteres")
        String nickname,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotBlank(message = "O número de celular é obrigatório")
        @Pattern(
                regexp = "^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$",
                message = "O número de celular deve seguir o formato (11) 91234-5678"
        )
        String numeroCelular,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotNull(message = "A idade é obrigatória")
        @Min(value = 12, message = "A idade mínima permitida é 12 anos")
        @Max(value = 100, message = "A idade máxima permitida é 100 anos")
        Integer idade,

        @NotNull(message = "A posição é obrigatória")
        Posicao posicao,

        @NotBlank(message = "A nacionalidade é obrigatória")
        @Size(max = 60, message = "A nacionalidade deve ter no máximo 60 caracteres")
        String nacionalidade

) {
}
