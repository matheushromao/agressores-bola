package com.hmz.agressores_da_bola.service.sorteio;

import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceadorDeTimesTest {

    private final BalanceadorDeTimes balanceador = new BalanceadorDeTimes();

    @Test
    @DisplayName("distribui as estrelas de forma equilibrada entre os times")
    void deveEquilibrarAsEstrelas() {
        // 10 jogadores somando 30 estrelas: a divisão perfeita é 15 para cada.
        List<JogadorSorteavel> jogadores = jogadoresDeLinha(
                "5.0", "5.0", "4.5", "4.0", "3.0", "3.0", "2.5", "1.5", "1.0", "0.5");

        TimesBalanceados resultado = balanceador.balancear(jogadores, 2, 5, new Random(7));

        assertThat(resultado.times()).hasSize(2);
        assertThat(resultado.times()).allSatisfy(time -> assertThat(time).hasSize(5));
        assertThat(resultado.diferencaEntreTimes()).isEqualByComparingTo("0.0");
    }

    @Test
    @DisplayName("mantém a diferença mínima mesmo quando a divisão exata é impossível")
    void deveMinimizarADiferencaQuandoNaoHaDivisaoExata() {
        // Soma 21.0 em 2 times: o melhor possível é 10.5 contra 10.5 se der,
        // senão meia estrela de diferença.
        List<JogadorSorteavel> jogadores = jogadoresDeLinha(
                "5.0", "4.5", "3.5", "3.0", "2.5", "2.5");

        TimesBalanceados resultado = balanceador.balancear(jogadores, 2, 3, new Random(42));

        assertThat(resultado.diferencaEntreTimes()).isLessThanOrEqualTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("dá no máximo um goleiro para cada time")
    void deveEspalharOsGoleiros() {
        List<JogadorSorteavel> jogadores = new ArrayList<>(jogadoresDeLinha(
                "4.0", "3.5", "3.0", "2.5", "2.0", "1.5"));
        jogadores.add(goleiro("Goleiro 1", "4.0"));
        jogadores.add(goleiro("Goleiro 2", "3.0"));

        TimesBalanceados resultado = balanceador.balancear(jogadores, 2, 4, new Random(1));

        assertThat(resultado.times()).allSatisfy(time ->
                assertThat(time).filteredOn(JogadorSorteavel::goleiro).hasSize(1));
    }

    @Test
    @DisplayName("deixa de reserva quem sobra da divisão exata")
    void deveMandarOExcedenteParaAsReservas() {
        List<JogadorSorteavel> jogadores = jogadoresDeLinha(
                "5.0", "4.0", "3.0", "3.0", "2.0", "1.0", "1.0");

        TimesBalanceados resultado = balanceador.balancear(jogadores, 2, 3, new Random(3));

        assertThat(resultado.times()).allSatisfy(time -> assertThat(time).hasSize(3));
        assertThat(resultado.reservas()).hasSize(1);
    }

    @Test
    @DisplayName("repete o mesmo sorteio quando a semente é a mesma")
    void deveSerReproduzivelComAMesmaSemente() {
        List<JogadorSorteavel> jogadores = jogadoresDeLinha(
                "4.0", "4.0", "3.0", "3.0", "2.0", "2.0");

        List<String> primeiro = nicknamesDoPrimeiroTime(balanceador.balancear(jogadores, 2, 3, new Random(99)));
        List<String> segundo = nicknamesDoPrimeiroTime(balanceador.balancear(jogadores, 2, 3, new Random(99)));

        assertThat(primeiro).isEqualTo(segundo);
    }

    @Test
    @DisplayName("trata jogador sem avaliação como mediano")
    void deveUsarAEstrelaPadraoDeQuemNaoFoiAvaliado() {
        Usuario semNota = usuario("sem-nota", Posicao.MEIA, null);

        JogadorSorteavel jogador = JogadorSorteavel.de(semNota);

        assertThat(jogador.estrelas()).isEqualByComparingTo(Usuario.ESTRELAS_PADRAO);
    }

    /* ------------------------------------------------------------------ */

    private List<JogadorSorteavel> jogadoresDeLinha(String... estrelas) {
        List<JogadorSorteavel> jogadores = new ArrayList<>(estrelas.length);
        for (int indice = 0; indice < estrelas.length; indice++) {
            jogadores.add(JogadorSorteavel.de(
                    usuario("jogador" + indice, Posicao.MEIA, new BigDecimal(estrelas[indice]))));
        }
        return jogadores;
    }

    private JogadorSorteavel goleiro(String nickname, String estrelas) {
        return JogadorSorteavel.de(usuario(nickname, Posicao.GOLEIRO, new BigDecimal(estrelas)));
    }

    private Usuario usuario(String nickname, Posicao posicao, BigDecimal estrelas) {
        return Usuario.builder()
                .id((long) nickname.hashCode())
                .nickname(nickname)
                .nomeCompleto(nickname)
                .posicao(posicao)
                .estrelas(estrelas)
                .build();
    }

    private List<String> nicknamesDoPrimeiroTime(TimesBalanceados resultado) {
        return resultado.times().getFirst().stream()
                .map(jogador -> jogador.usuario().getNickname())
                .sorted()
                .toList();
    }
}
