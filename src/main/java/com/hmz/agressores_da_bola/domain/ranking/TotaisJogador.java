package com.hmz.agressores_da_bola.domain.ranking;

import com.hmz.agressores_da_bola.domain.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;

import java.math.BigDecimal;

/**
 * Linha bruta do somatório de estatísticas por jogador, montada direto pelo
 * banco com {@code group by}. A pontuação não vem daqui: o banco só soma as
 * quantidades e os pesos continuam aplicados em um lugar só, no domínio.
 *
 * <p>Os totais chegam como {@code Long} porque {@code sum} e {@code count} do
 * JPQL são de precisão longa — e como objeto, não primitivo, para o Hibernate
 * casar a expressão de construtor sem depender de autoboxing.</p>
 */
public record TotaisJogador(
        Long usuarioId,
        String nickname,
        String nomeCompleto,
        Posicao posicao,
        BigDecimal estrelas,
        Long jogos,
        Long gols,
        Long assistencias,
        Long desarmes,
        Long defesas,
        Long defesasDificeis
) {

    public ResumoEstatistico resumo() {
        return new ResumoEstatistico(
                Math.toIntExact(gols),
                Math.toIntExact(assistencias),
                Math.toIntExact(desarmes),
                Math.toIntExact(defesas),
                Math.toIntExact(defesasDificeis)
        );
    }

    public int pontuacao() {
        return resumo().pontuacao();
    }

    public int quantidadeDe(AtributoPontuacao atributo) {
        return resumo().quantidadeDe(atributo);
    }

    /**
     * Quem não marcou não entra na artilharia: uma lista de artilheiros com
     * zero gols só polui a tela.
     */
    public boolean pontuouEm(AtributoPontuacao atributo) {
        return quantidadeDe(atributo) > 0;
    }
}
