package com.hmz.agressores_da_bola.model;

import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;

/**
 * Objeto de valor com os números de um jogador. Existe para que a pontuação
 * seja calculada em um único lugar, tanto para a estatística de uma pelada
 * quanto para os totais somados de várias peladas no ranking.
 */
public record ResumoEstatistico(
        int gols,
        int assistencias,
        int desarmes,
        int defesas,
        int defesasDificeis
) {

    public static final ResumoEstatistico ZERO = new ResumoEstatistico(0, 0, 0, 0, 0);

    public ResumoEstatistico mais(ResumoEstatistico outro) {
        return new ResumoEstatistico(
                gols + outro.gols,
                assistencias + outro.assistencias,
                desarmes + outro.desarmes,
                defesas + outro.defesas,
                defesasDificeis + outro.defesasDificeis
        );
    }

    public int quantidadeDe(AtributoPontuacao atributo) {
        return atributo.quantidade(this);
    }

    /**
     * Soma os pontos de todos os atributos aplicando o peso de cada um.
     */
    public int pontuacao() {
        int total = 0;
        for (AtributoPontuacao atributo : AtributoPontuacao.values()) {
            total += atributo.pontos(this);
        }
        return total;
    }
}
