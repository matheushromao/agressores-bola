package com.hmz.agressores_da_bola.domain.ranking;

import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;

import java.util.Comparator;

/**
 * Critérios de desempate dos rankings. Ficam no domínio porque são regra da
 * liga, não detalhe de apresentação.
 */
public final class OrdenacaoDoRanking {

    private OrdenacaoDoRanking() {
    }

    /**
     * Pontos decidem; no empate vale quem fez mais gols, depois quem deu mais
     * assistências e, por fim, a ordem alfabética para o resultado ser estável.
     */
    public static Comparator<TotaisJogador> geral() {
        Comparator<TotaisJogador> porPontos = Comparator.comparingInt(TotaisJogador::pontuacao);
        Comparator<TotaisJogador> porGols = Comparator.comparingLong(TotaisJogador::gols);
        Comparator<TotaisJogador> porAssistencias = Comparator.comparingLong(TotaisJogador::assistencias);

        return porPontos.reversed()
                .thenComparing(porGols.reversed())
                .thenComparing(porAssistencias.reversed())
                .thenComparing(TotaisJogador::nickname, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * No ranking de um atributo, o empate é desfeito por quem fez mais com
     * menos jogos e depois pela pontuação geral.
     */
    public static Comparator<TotaisJogador> porAtributo(AtributoPontuacao atributo) {
        Comparator<TotaisJogador> porQuantidade =
                Comparator.comparingInt(linha -> linha.quantidadeDe(atributo));
        Comparator<TotaisJogador> porJogos = Comparator.comparingLong(TotaisJogador::jogos);
        Comparator<TotaisJogador> porPontos = Comparator.comparingInt(TotaisJogador::pontuacao);

        return porQuantidade.reversed()
                .thenComparing(porJogos)
                .thenComparing(porPontos.reversed())
                .thenComparing(TotaisJogador::nickname, String.CASE_INSENSITIVE_ORDER);
    }
}
