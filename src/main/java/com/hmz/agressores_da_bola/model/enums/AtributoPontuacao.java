package com.hmz.agressores_da_bola.model.enums;

import com.hmz.agressores_da_bola.model.ResumoEstatistico;

import java.util.function.ToIntFunction;

/**
 * Tabela de pontuação da liga. Concentra em um só lugar o peso de cada
 * atributo e como extrair a quantidade dele de um {@link ResumoEstatistico},
 * de modo que a pontuação individual, o ranking geral e os rankings por
 * atributo compartilhem exatamente a mesma regra.
 *
 * <p>Os pesos seguem a dificuldade da jogada: o gol decide a partida e vale
 * mais; a defesa difícil vale quase um gol porque evita um; o desarme e a
 * defesa comum são o trabalho de base e valem menos.</p>
 */
public enum AtributoPontuacao {

    GOL("Gols", 10, ResumoEstatistico::gols),
    DEFESA_DIFICIL("Defesas difíceis", 8, ResumoEstatistico::defesasDificeis),
    ASSISTENCIA("Assistências", 7, ResumoEstatistico::assistencias),
    DEFESA("Defesas", 4, ResumoEstatistico::defesas),
    DESARME("Desarmes", 3, ResumoEstatistico::desarmes);

    private final String descricao;
    private final int peso;
    private final ToIntFunction<ResumoEstatistico> leitor;

    AtributoPontuacao(String descricao, int peso, ToIntFunction<ResumoEstatistico> leitor) {
        this.descricao = descricao;
        this.peso = peso;
        this.leitor = leitor;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPeso() {
        return peso;
    }

    public int quantidade(ResumoEstatistico resumo) {
        return leitor.applyAsInt(resumo);
    }

    public int pontos(ResumoEstatistico resumo) {
        return quantidade(resumo) * peso;
    }

    /**
     * Defesas só existem para quem jogou no gol.
     */
    public boolean exclusivoDeGoleiro() {
        return this == DEFESA || this == DEFESA_DIFICIL;
    }

    /**
     * Desarme é estatística de jogador de linha.
     */
    public boolean exclusivoDeLinha() {
        return this == DESARME;
    }
}
