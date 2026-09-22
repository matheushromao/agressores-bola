package com.hmz.agressores_da_bola.domain.sorteio;

import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Como o critério pedido vira times, e o que o organizador lê quando a
 * divisão não fecha.
 */
class DivisaoDeTimesTest {

    @Test
    @DisplayName("pedindo a quantidade de times, sobra reserva e os times ficam do mesmo tamanho")
    void porQuantidadeDeTimes() {
        // 11 confirmados em 2 times: 5 por time, e o 11º vira reserva
        assertThat(DivisaoDeTimes.resolver(2, null, 11)).isEqualTo(new DivisaoDeTimes(2, 5));
    }

    @Test
    @DisplayName("pedindo o tamanho do time, a quantidade é o que couber nos confirmados")
    void porJogadoresPorTime() {
        assertThat(DivisaoDeTimes.resolver(null, 5, 17)).isEqualTo(new DivisaoDeTimes(3, 5));
    }

    /**
     * O caso que motivou a mensagem: falar do mínimo absoluto do sorteio ("pelo
     * menos 4 para formar 2 times de 2") é verdade e não ajuda em nada quem
     * pediu times de 6.
     */
    @Test
    @DisplayName("faltando gente para o tamanho de time pedido, a mensagem diz quanto falta")
    void mensagemCitaOTamanhoDeTimePedido() {
        assertThatThrownBy(() -> DivisaoDeTimes.resolver(null, 6, 10))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("times de 6")
                .hasMessageContaining("pelo menos 12")
                .hasMessageContaining("a pelada tem 10");
    }

    @Test
    @DisplayName("faltando gente para a quantidade de times pedida, a mensagem diz quanto falta")
    void mensagemCitaAQuantidadeDeTimesPedida() {
        assertThatThrownBy(() -> DivisaoDeTimes.resolver(6, null, 10))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("6 times")
                .hasMessageContaining("pelo menos 12")
                .hasMessageContaining("a pelada tem 10");
    }
}
