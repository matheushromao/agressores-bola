package com.hmz.agressores_da_bola.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuração do sorteio. O organizador escolhe um dos dois critérios:
 * quantos times quer formar, ou quantos jogadores cada time deve ter.
 * O outro valor é deduzido a partir da quantidade de confirmados.
 */
@Schema(description = "Critério do sorteio: informe quantidadeTimes OU jogadoresPorTime, "
        + "nunca os dois. O outro valor é deduzido dos confirmados.")
public record SorteioRequest(

        @Schema(description = "Quantos times formar", example = "2")
        @Min(value = 2, message = "O sorteio precisa de pelo menos 2 times")
        @Max(value = 10, message = "O sorteio aceita no máximo 10 times")
        Integer quantidadeTimes,

        @Schema(description = "Quantos jogadores por time", example = "5")
        @Min(value = 2, message = "Cada time precisa de pelo menos 2 jogadores")
        @Max(value = 11, message = "Cada time aceita no máximo 11 jogadores")
        Integer jogadoresPorTime,

        // Opcional: repetir a mesma semente reproduz exatamente o mesmo
        // sorteio, útil para conferir ou refazer uma divisão já combinada.
        @Schema(description = "Semente do sorteio. A mesma semente reproduz exatamente a mesma "
                + "divisão, útil para refazer ou conferir um sorteio já combinado.",
                example = "42")
        Long semente

) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Informe a quantidade de times ou os jogadores por time, e apenas um dos dois")
    public boolean isCriterioValido() {
        return (quantidadeTimes == null) != (jogadoresPorTime == null);
    }
}
