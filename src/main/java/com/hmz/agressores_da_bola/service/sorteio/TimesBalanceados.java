package com.hmz.agressores_da_bola.service.sorteio;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Saída do balanceamento: os times formados e quem ficou de fora da divisão
 * exata.
 */
public record TimesBalanceados(List<List<JogadorSorteavel>> times, List<JogadorSorteavel> reservas) {

    /**
     * Distância em estrelas entre o time mais forte e o mais fraco. É a
     * medida de quão justo o sorteio ficou: zero significa times de força
     * idêntica no papel.
     */
    public BigDecimal diferencaEntreTimes() {
        if (times.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> forcas = times.stream()
                .map(TimesBalanceados::somaEstrelas)
                .sorted()
                .toList();

        return forcas.getLast().subtract(forcas.getFirst());
    }

    public static BigDecimal somaEstrelas(List<JogadorSorteavel> time) {
        return time.stream()
                .map(JogadorSorteavel::estrelas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Ordena os jogadores de um time do mais bem avaliado para o menos, com o
     * goleiro sempre na frente, que é como o time aparece na escalação.
     */
    public static Comparator<JogadorSorteavel> ordemDeExibicao() {
        Comparator<JogadorSorteavel> goleiroPrimeiro =
                Comparator.comparing(JogadorSorteavel::goleiro).reversed();
        Comparator<JogadorSorteavel> porEstrelas =
                Comparator.comparing(JogadorSorteavel::estrelas).reversed();
        Comparator<JogadorSorteavel> porNickname =
                Comparator.comparing(jogador -> jogador.usuario().getNickname());

        return goleiroPrimeiro.thenComparing(porEstrelas).thenComparing(porNickname);
    }
}
