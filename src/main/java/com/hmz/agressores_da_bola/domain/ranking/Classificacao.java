package com.hmz.agressores_da_bola.domain.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * Monta uma tabela de classificação: ordena, corta pelo limite e distribui
 * as colocações. Empate divide a posição e pula a seguinte (1, 2, 2, 4),
 * como em qualquer tabela.
 */
public final class Classificacao {

    private Classificacao() {
    }

    /**
     * @param criterioDeEmpate valor que, repetido, faz duas linhas dividirem a colocação
     * @param limite           quantas linhas manter; nulo ou não positivo mantém todas
     * @param montador         recebe a linha e a colocação e devolve o item da tabela
     */
    public static <L, T> List<T> classificar(List<L> linhas,
                                             Comparator<L> ordenacao,
                                             ToIntFunction<L> criterioDeEmpate,
                                             Integer limite,
                                             BiFunction<L, Integer, T> montador) {

        List<L> ordenadas = linhas.stream()
                .sorted(ordenacao)
                .limit(limiteEfetivo(limite, linhas.size()))
                .toList();

        List<T> classificacao = new ArrayList<>(ordenadas.size());
        int posicao = 0;
        Integer valorAnterior = null;

        for (int indice = 0; indice < ordenadas.size(); indice++) {
            L linha = ordenadas.get(indice);
            int valor = criterioDeEmpate.applyAsInt(linha);

            if (valorAnterior == null || valor != valorAnterior) {
                posicao = indice + 1;
                valorAnterior = valor;
            }
            classificacao.add(montador.apply(linha, posicao));
        }
        return classificacao;
    }

    private static long limiteEfetivo(Integer limite, int total) {
        return limite == null || limite <= 0 ? total : limite;
    }
}
