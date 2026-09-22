package com.hmz.agressores_da_bola.domain.sorteio;

import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;

/**
 * Quantos times e de quantos jogadores. O organizador escolhe um dos dois
 * critérios e o outro é deduzido dos confirmados.
 */
public record DivisaoDeTimes(int quantidadeTimes, int jogadoresPorTime) {

    public static final int MINIMO_DE_TIMES = 2;
    public static final int MINIMO_DE_JOGADORES_POR_TIME = 2;

    /**
     * @param quantidadeTimesPedida  preenchido quando o organizador pediu N times
     * @param jogadoresPorTimePedido preenchido quando pediu times de N jogadores
     * @param confirmados            quantos jogadores confirmados a pelada tem
     */
    public static DivisaoDeTimes resolver(Integer quantidadeTimesPedida,
                                          Integer jogadoresPorTimePedido,
                                          int confirmados) {
        int quantidadeTimes = quantidadeTimesPedida != null
                ? quantidadeTimesPedida
                // Times de tamanho fixo: a quantidade é o que couber nos
                // confirmados — o resto vira reserva.
                : confirmados / jogadoresPorTimePedido;

        int jogadoresPorTime = jogadoresPorTimePedido != null
                ? jogadoresPorTimePedido
                // Times de tamanho igual: os que sobram da divisão ficam de
                // reserva, porque um time com um jogador a mais já nasce em vantagem.
                : quantidadeTimes == 0 ? 0 : confirmados / quantidadeTimes;

        if (quantidadeTimes < MINIMO_DE_TIMES || jogadoresPorTime < MINIMO_DE_JOGADORES_POR_TIME) {
            throw faltamJogadores(quantidadeTimesPedida, jogadoresPorTimePedido, confirmados);
        }
        return new DivisaoDeTimes(quantidadeTimes, jogadoresPorTime);
    }

    /**
     * Só existe um jeito de a divisão não fechar: faltar gente para o critério
     * pedido. O outro caso imaginável — confirmados a menos do que
     * {@code times × jogadoresPorTime} — não acontece, porque o valor deduzido
     * sempre vem de uma divisão inteira e o resto vira reserva.
     *
     * <p>A mensagem cita <strong>o que o organizador pediu</strong>, não o
     * mínimo absoluto do sorteio: quem pede times de 6 com 10 confirmados
     * precisa ouvir que faltam 2 jogadores, e não que "é preciso pelo menos 4
     * para formar 2 times de 2" — verdadeiro, porém inútil.
     */
    private static RegraDeNegocioException faltamJogadores(Integer quantidadeTimesPedida,
                                                           Integer jogadoresPorTimePedido,
                                                           int confirmados) {
        if (quantidadeTimesPedida != null) {
            int necessarios = quantidadeTimesPedida * MINIMO_DE_JOGADORES_POR_TIME;
            return new RegraDeNegocioException(
                    "Para formar " + quantidadeTimesPedida + " times são necessários pelo menos "
                            + necessarios + " jogadores confirmados, já que cada time precisa de "
                            + MINIMO_DE_JOGADORES_POR_TIME + ", mas a pelada tem " + confirmados);
        }

        int necessarios = jogadoresPorTimePedido * MINIMO_DE_TIMES;
        return new RegraDeNegocioException(
                "Para formar times de " + jogadoresPorTimePedido
                        + " jogadores são necessários pelo menos " + necessarios
                        + " confirmados, já que o sorteio precisa de ao menos "
                        + MINIMO_DE_TIMES + " times, mas a pelada tem " + confirmados);
    }
}
