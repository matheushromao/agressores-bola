package com.hmz.agressores_da_bola.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmz.agressores_da_bola.application.estatistica.LancamentoEstatistica;
import com.hmz.agressores_da_bola.domain.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Lançamento da súmula de um jogador na pelada. Todos os números são
 * opcionais e valem zero quando omitidos — o organizador digita só o que
 * aconteceu.
 */
@Schema(description = "Súmula de um jogador. Todo número omitido vale zero.")
public record EstatisticaRequest(

        @Schema(description = "Gols marcados", example = "2")
        @Min(value = 0, message = "O número de gols não pode ser negativo")
        @Max(value = 50, message = "O número de gols informado é irreal para uma pelada")
        Integer gols,

        @Schema(description = "Assistências", example = "1")
        @Min(value = 0, message = "O número de assistências não pode ser negativo")
        @Max(value = 50, message = "O número de assistências informado é irreal para uma pelada")
        Integer assistencias,

        @Schema(description = "Desarmes", example = "4")
        @Min(value = 0, message = "O número de desarmes não pode ser negativo")
        @Max(value = 100, message = "O número de desarmes informado é irreal para uma pelada")
        Integer desarmes,

        @Schema(description = "Defesas; só para quem jogou no gol", example = "7")
        @Min(value = 0, message = "O número de defesas não pode ser negativo")
        @Max(value = 200, message = "O número de defesas informado é irreal para uma pelada")
        Integer defesas,

        @Schema(description = "Defesas difíceis; subconjunto das defesas, nunca maior que elas",
                example = "2")
        @Min(value = 0, message = "O número de defesas difíceis não pode ser negativo")
        @Max(value = 100, message = "O número de defesas difíceis informado é irreal para uma pelada")
        Integer defesasDificeis,

        // Opcional: quando omitida, vale a posição do cadastro do jogador.
        // Serve para o caso do jogador de linha que foi para o gol.
        @Schema(description = "Posição efetivamente jogada. Omitida, vale a do cadastro — "
                + "serve para o jogador de linha que foi para o gol.")
        Posicao posicaoJogada

) {

    public LancamentoEstatistica paraLancamento() {
        ResumoEstatistico numeros = new ResumoEstatistico(
                zeroSeNulo(gols),
                zeroSeNulo(assistencias),
                zeroSeNulo(desarmes),
                zeroSeNulo(defesas),
                zeroSeNulo(defesasDificeis)
        );
        return new LancamentoEstatistica(numeros, posicaoJogada);
    }

    /**
     * Defesa difícil é um subconjunto das defesas: toda defesa difícil também
     * é uma defesa, então nunca pode haver mais delas do que o total.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "As defesas difíceis não podem superar o total de defesas")
    public boolean isDefesasCoerentes() {
        return zeroSeNulo(defesasDificeis) <= zeroSeNulo(defesas);
    }

    private static int zeroSeNulo(Integer valor) {
        return valor == null ? 0 : valor;
    }
}
