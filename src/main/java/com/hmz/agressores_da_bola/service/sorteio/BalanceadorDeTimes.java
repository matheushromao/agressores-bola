package com.hmz.agressores_da_bola.service.sorteio;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Divide os confirmados em times equilibrados pela soma de estrelas.
 *
 * <p>É um sorteio de verdade, mas não um sorteio cego: o acaso decide quem
 * joga com quem entre jogadores de mesmo nível, enquanto as estrelas garantem
 * que nenhum time saia muito mais forte que o outro. A estratégia tem três
 * etapas:</p>
 *
 * <ol>
 *   <li><b>goleiros primeiro</b> — no máximo um por time, porque dois goleiros
 *       no mesmo time desequilibram muito mais que qualquer nota;</li>
 *   <li><b>distribuição gulosa</b> — os jogadores entram do mais estrelado
 *       para o menos, cada um no time mais fraco naquele momento. Sozinha essa
 *       regra já costuma parar perto do ideal;</li>
 *   <li><b>refino por trocas</b> — troca-se um jogador do time mais forte por
 *       um do mais fraco sempre que isso encurtar a diferença, até não haver
 *       mais troca que melhore.</li>
 * </ol>
 *
 * <p>A classe é isolada de JPA e de Spring de propósito: recebe a lista e o
 * {@link Random}, devolve os times, e por isso pode ser testada sozinha.</p>
 */
@Component
public class BalanceadorDeTimes {

    /**
     * Teto de trocas do refino. Cada troca só é aceita se reduzir a diferença,
     * então o processo termina sozinho; o limite existe apenas como rede de
     * segurança contra um laço infinito por empate numérico.
     */
    private static final int MAXIMO_DE_TROCAS = 100;

    private static final BigDecimal DOIS = BigDecimal.valueOf(2);

    public TimesBalanceados balancear(List<JogadorSorteavel> confirmados,
                                      int quantidadeTimes,
                                      int jogadoresPorTime,
                                      Random random) {

        List<JogadorSorteavel> sorteados = new ArrayList<>(confirmados);
        Collections.shuffle(sorteados, random);

        List<JogadorSorteavel> goleiros = new ArrayList<>();
        List<JogadorSorteavel> linha = new ArrayList<>();
        separarGoleiros(sorteados, quantidadeTimes, goleiros, linha);

        int vagasNaLinha = Math.max(0, quantidadeTimes * jogadoresPorTime - goleiros.size());
        int titulares = Math.min(vagasNaLinha, linha.size());

        List<JogadorSorteavel> escalados = new ArrayList<>(linha.subList(0, titulares));
        List<JogadorSorteavel> reservas = new ArrayList<>(linha.subList(titulares, linha.size()));

        List<List<JogadorSorteavel>> times = new ArrayList<>(quantidadeTimes);
        for (int indice = 0; indice < quantidadeTimes; indice++) {
            times.add(new ArrayList<>(jogadoresPorTime));
        }

        // Um goleiro por time. Como a lista já veio embaralhada, qual goleiro
        // cai em qual time é sorte.
        for (int indice = 0; indice < goleiros.size(); indice++) {
            times.get(indice).add(goleiros.get(indice));
        }

        distribuirPorForca(escalados, times, jogadoresPorTime);
        refinarPorTrocas(times);

        return new TimesBalanceados(times, reservas);
    }

    /* ------------------------------------------------------------------
     * Etapas do balanceamento
     * ------------------------------------------------------------------ */

    /**
     * Separa até um goleiro por time. Goleiro excedente volta para o bolo da
     * linha e disputa vaga como qualquer outro — é o que acontece na pelada.
     */
    private void separarGoleiros(List<JogadorSorteavel> sorteados,
                                 int quantidadeTimes,
                                 List<JogadorSorteavel> goleiros,
                                 List<JogadorSorteavel> linha) {
        for (JogadorSorteavel jogador : sorteados) {
            if (jogador.goleiro() && goleiros.size() < quantidadeTimes) {
                goleiros.add(jogador);
            } else {
                linha.add(jogador);
            }
        }
    }

    /**
     * Do mais estrelado para o menos, cada jogador entra no time mais fraco
     * com vaga. A ordenação é estável, então jogadores de mesma nota mantêm a
     * ordem aleatória do embaralhamento — é aí que mora o sorteio.
     */
    private void distribuirPorForca(List<JogadorSorteavel> escalados,
                                    List<List<JogadorSorteavel>> times,
                                    int jogadoresPorTime) {
        List<JogadorSorteavel> porForca = new ArrayList<>(escalados);
        porForca.sort(Comparator.comparing(JogadorSorteavel::estrelas).reversed());

        for (JogadorSorteavel jogador : porForca) {
            times.get(indiceDoTimeMaisFraco(times, jogadoresPorTime)).add(jogador);
        }
    }

    private int indiceDoTimeMaisFraco(List<List<JogadorSorteavel>> times, int jogadoresPorTime) {
        int escolhido = -1;
        BigDecimal menorForca = null;
        int menorTamanho = Integer.MAX_VALUE;

        for (int indice = 0; indice < times.size(); indice++) {
            List<JogadorSorteavel> time = times.get(indice);
            if (time.size() >= jogadoresPorTime) {
                continue;
            }
            BigDecimal forca = TimesBalanceados.somaEstrelas(time);

            // Menor força vence; no empate, o time com menos jogadores, para
            // os elencos não ficarem desiguais no meio da distribuição.
            boolean melhor = menorForca == null
                    || forca.compareTo(menorForca) < 0
                    || (forca.compareTo(menorForca) == 0 && time.size() < menorTamanho);

            if (melhor) {
                escolhido = indice;
                menorForca = forca;
                menorTamanho = time.size();
            }
        }
        // Só acontece se não houver vaga alguma, o que o service já impede.
        return escolhido < 0 ? 0 : escolhido;
    }

    /**
     * Enquanto existir uma troca entre o time mais forte e o mais fraco que
     * encurte a diferença, faz a troca.
     */
    private void refinarPorTrocas(List<List<JogadorSorteavel>> times) {
        for (int tentativa = 0; tentativa < MAXIMO_DE_TROCAS; tentativa++) {
            if (!trocarMelhorPar(times)) {
                return;
            }
        }
    }

    private boolean trocarMelhorPar(List<List<JogadorSorteavel>> times) {
        int indiceForte = 0;
        int indiceFraco = 0;

        for (int indice = 1; indice < times.size(); indice++) {
            BigDecimal forca = TimesBalanceados.somaEstrelas(times.get(indice));
            if (forca.compareTo(TimesBalanceados.somaEstrelas(times.get(indiceForte))) > 0) {
                indiceForte = indice;
            }
            if (forca.compareTo(TimesBalanceados.somaEstrelas(times.get(indiceFraco))) < 0) {
                indiceFraco = indice;
            }
        }

        List<JogadorSorteavel> forte = times.get(indiceForte);
        List<JogadorSorteavel> fraco = times.get(indiceFraco);

        BigDecimal diferenca = TimesBalanceados.somaEstrelas(forte)
                .subtract(TimesBalanceados.somaEstrelas(fraco));

        if (diferenca.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        int melhorNoForte = -1;
        int melhorNoFraco = -1;
        BigDecimal melhorDiferenca = diferenca;

        for (int posicaoForte = 0; posicaoForte < forte.size(); posicaoForte++) {
            JogadorSorteavel doForte = forte.get(posicaoForte);
            // Goleiro não entra na troca: sair dele quebraria a regra de um
            // por time, que vale mais do que uns décimos de estrela.
            if (doForte.goleiro()) {
                continue;
            }
            for (int posicaoFraco = 0; posicaoFraco < fraco.size(); posicaoFraco++) {
                JogadorSorteavel doFraco = fraco.get(posicaoFraco);
                if (doFraco.goleiro()) {
                    continue;
                }

                // Trocar dois jogadores move 2x a distância entre as notas de
                // um lado para o outro.
                BigDecimal novaDiferenca = diferenca
                        .subtract(doForte.estrelas().subtract(doFraco.estrelas()).multiply(DOIS))
                        .abs();

                if (novaDiferenca.compareTo(melhorDiferenca) < 0) {
                    melhorDiferenca = novaDiferenca;
                    melhorNoForte = posicaoForte;
                    melhorNoFraco = posicaoFraco;
                }
            }
        }

        if (melhorNoForte < 0) {
            return false;
        }

        JogadorSorteavel doForte = forte.get(melhorNoForte);
        forte.set(melhorNoForte, fraco.get(melhorNoFraco));
        fraco.set(melhorNoFraco, doForte);
        return true;
    }
}
