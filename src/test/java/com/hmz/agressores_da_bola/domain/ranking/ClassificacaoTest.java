package com.hmz.agressores_da_bola.domain.ranking;

import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoTest {

    private record Colocado(String nickname, int posicao) {
    }

    @Test
    @DisplayName("empate divide a colocação e pula a seguinte, como em qualquer tabela")
    void empateDivideAColocacao() {
        List<Colocado> tabela = classificarPorGols(List.of(
                linha("dudu", 3, 1), linha("ana", 5, 1), linha("bia", 3, 1), linha("caio", 1, 1)), null);

        assertThat(tabela).containsExactly(
                new Colocado("ana", 1),
                new Colocado("bia", 2),
                new Colocado("dudu", 2),
                new Colocado("caio", 4));
    }

    @Test
    @DisplayName("o limite corta a tabela sem mexer nas colocações")
    void limiteCortaATabela() {
        List<Colocado> tabela = classificarPorGols(List.of(
                linha("ana", 5, 1), linha("bia", 3, 1), linha("caio", 1, 1)), 2);

        assertThat(tabela).extracting(Colocado::nickname).containsExactly("ana", "bia");
    }

    @Test
    @DisplayName("limite nulo ou não positivo traz a tabela inteira")
    void limiteNaoPositivoTrazTudo() {
        List<TotaisJogador> linhas = List.of(linha("ana", 5, 1), linha("bia", 3, 1));

        assertThat(classificarPorGols(linhas, 0)).hasSize(2);
        assertThat(classificarPorGols(linhas, null)).hasSize(2);
    }

    @Test
    @DisplayName("no ranking do atributo, quem fez o mesmo com menos jogos fica na frente")
    void desempatePorMenosJogos() {
        List<TotaisJogador> ordenadas = List.of(linha("veterano", 4, 6), linha("estreante", 4, 2)).stream()
                .sorted(OrdenacaoDoRanking.porAtributo(AtributoPontuacao.GOL))
                .toList();

        assertThat(ordenadas).extracting(TotaisJogador::nickname).containsExactly("estreante", "veterano");
    }

    /* ------------------------------------------------------------------ */

    private static List<Colocado> classificarPorGols(List<TotaisJogador> linhas, Integer limite) {
        return Classificacao.classificar(
                linhas,
                OrdenacaoDoRanking.geral(),
                TotaisJogador::pontuacao,
                limite,
                (linha, posicao) -> new Colocado(linha.nickname(), posicao));
    }

    private static TotaisJogador linha(String nickname, long gols, long jogos) {
        return new TotaisJogador((long) nickname.hashCode(), nickname, nickname, Posicao.ALA,
                BigDecimal.valueOf(3), jogos, gols, 0L, 0L, 0L, 0L);
    }
}
