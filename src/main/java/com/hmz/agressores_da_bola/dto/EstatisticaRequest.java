package com.hmz.agressores_da_bola.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmz.agressores_da_bola.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Lançamento da súmula de um jogador na pelada. Todos os números são
 * opcionais e valem zero quando omitidos — o organizador digita só o que
 * aconteceu.
 */
public record EstatisticaRequest(

        @Min(value = 0, message = "O número de gols não pode ser negativo")
        @Max(value = 50, message = "O número de gols informado é irreal para uma pelada")
        Integer gols,

        @Min(value = 0, message = "O número de assistências não pode ser negativo")
        @Max(value = 50, message = "O número de assistências informado é irreal para uma pelada")
        Integer assistencias,

        @Min(value = 0, message = "O número de desarmes não pode ser negativo")
        @Max(value = 100, message = "O número de desarmes informado é irreal para uma pelada")
        Integer desarmes,

        @Min(value = 0, message = "O número de defesas não pode ser negativo")
        @Max(value = 200, message = "O número de defesas informado é irreal para uma pelada")
        Integer defesas,

        @Min(value = 0, message = "O número de defesas difíceis não pode ser negativo")
        @Max(value = 100, message = "O número de defesas difíceis informado é irreal para uma pelada")
        Integer defesasDificeis,

        // Opcional: quando omitida, vale a posição do cadastro do jogador.
        // Serve para o caso do jogador de linha que foi para o gol.
        Posicao posicaoJogada

) {

    public ResumoEstatistico paraResumo() {
        return new ResumoEstatistico(
                zeroSeNulo(gols),
                zeroSeNulo(assistencias),
                zeroSeNulo(desarmes),
                zeroSeNulo(defesas),
                zeroSeNulo(defesasDificeis)
        );
    }

    /**
     * Defesa difícil é um subconjunto das defesas: toda defesa difícil também
     * é uma defesa, então nunca pode haver mais delas do que o total.
     */
    @JsonIgnore
    @AssertTrue(message = "As defesas difíceis não podem superar o total de defesas")
    public boolean isDefesasCoerentes() {
        return zeroSeNulo(defesasDificeis) <= zeroSeNulo(defesas);
    }

    private static int zeroSeNulo(Integer valor) {
        return valor == null ? 0 : valor;
    }
}
