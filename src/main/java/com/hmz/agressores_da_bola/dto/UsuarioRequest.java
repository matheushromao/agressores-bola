package com.hmz.agressores_da_bola.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Perfil de jogador")
public record UsuarioRequest(

        @Schema(description = "Nome completo", example = "Matheus Romão")
        @NotBlank(message = "O nome completo é obrigatório")
        @Size(min = 3, max = 120, message = "O nome completo deve ter entre 3 e 120 caracteres")
        String nomeCompleto,

        @Schema(description = "Apelido único dentro da liga", example = "hmz")
        @NotBlank(message = "O nickname é obrigatório")
        @Size(min = 3, max = 30, message = "O nickname deve ter entre 3 e 30 caracteres")
        String nickname,

        @Schema(description = "Texto livre de apresentação", example = "Joga de ala desde 2015")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(description = "Celular com DDD", example = "(11) 91234-5678")
        @NotBlank(message = "O número de celular é obrigatório")
        @Pattern(
                regexp = "^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$",
                message = "O número de celular deve seguir o formato (11) 91234-5678"
        )
        String numeroCelular,

        @Schema(description = "E-mail único, usado também no login", example = "matheus@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Schema(description = "Idade em anos", example = "27")
        @NotNull(message = "A idade é obrigatória")
        @Min(value = 12, message = "A idade mínima permitida é 12 anos")
        @Max(value = 100, message = "A idade máxima permitida é 100 anos")
        Integer idade,

        @Schema(description = "Posição em quadra")
        @NotNull(message = "A posição é obrigatória")
        Posicao posicao,

        @Schema(description = "Nacionalidade", example = "Brasileiro")
        @NotBlank(message = "A nacionalidade é obrigatória")
        @Size(max = 60, message = "A nacionalidade deve ter no máximo 60 caracteres")
        String nacionalidade,

        // Opcional: quem não é avaliado entra como jogador mediano (3 estrelas).
        @Schema(description = "Nível técnico de 1 a 5, de meia em meia estrela. "
                + "Omitido, o jogador entra como mediano (3). É o peso usado no sorteio.",
                example = "3.5")
        @DecimalMin(value = "1.0", message = "A classificação mínima é 1 estrela")
        @DecimalMax(value = "5.0", message = "A classificação máxima é 5 estrelas")
        BigDecimal estrelas

) {

    /**
     * A escala aceita apenas notas cheias e meias notas (1, 1.5, 2, ... 5).
     * O Bean Validation não expressa "múltiplo de", então a checagem vem aqui.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "A classificação deve variar de meia em meia estrela, por exemplo 3.5")
    public boolean isEstrelasNaEscala() {
        return estrelas == null
                || estrelas.stripTrailingZeros().scale() <= 1
                && estrelas.remainder(PASSO_DA_ESCALA).compareTo(BigDecimal.ZERO) == 0;
    }

    private static final BigDecimal PASSO_DA_ESCALA = new BigDecimal("0.5");
}
