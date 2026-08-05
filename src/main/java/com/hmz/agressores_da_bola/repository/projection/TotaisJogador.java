package com.hmz.agressores_da_bola.repository.projection;

import com.hmz.agressores_da_bola.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.model.enums.Posicao;

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
}
