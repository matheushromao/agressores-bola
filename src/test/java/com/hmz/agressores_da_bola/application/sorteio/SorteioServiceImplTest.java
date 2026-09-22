package com.hmz.agressores_da_bola.application.sorteio;

import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.CenarioDePelada;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.domain.sorteio.BalanceadorDeTimes;
import com.hmz.agressores_da_bola.domain.sorteio.TimesBalanceados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Random;

import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.ORGANIZADOR_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.PELADA_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.usuario;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestração do sorteio: quem pode pedir, quando, e o que chega ao
 * balanceador. As mensagens de divisão que não fecha estão em
 * {@code DivisaoDeTimesTest}.
 */
@ExtendWith(MockitoExtension.class)
class SorteioServiceImplTest {

    @Mock private PeladaRepository peladaRepository;
    @Mock private BalanceadorDeTimes balanceador;
    @Mock private SorteioMapper sorteioMapper;

    private SorteioServiceImpl service;

    @BeforeEach
    void criarService() {
        service = new SorteioServiceImpl(peladaRepository, balanceador, sorteioMapper);
    }

    @Test
    @DisplayName("só o organizador sorteia")
    void apenasOrganizadorSorteia() {
        peladaCom(10, StatusPelada.AGENDADA);

        assertThatThrownBy(() -> service.sortear(PELADA_ID, porTimes(2), 999L))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Só o organizador");

        verify(balanceador, never()).balancear(anyList(), anyInt(), anyInt(), any(Random.class));
    }

    @Test
    @DisplayName("pelada finalizada não se sorteia mais")
    void peladaFinalizadaNaoSorteia() {
        peladaCom(10, StatusPelada.FINALIZADA);

        assertThatThrownBy(() -> service.sortear(PELADA_ID, porTimes(2), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não faz mais sentido sortear");
    }

    @Test
    @DisplayName("convidado e lista de espera não entram na conta")
    void soConfirmadosEntram() {
        CenarioDePelada cenario = peladaCom(4, StatusPelada.AGENDADA);
        cenario.com(usuario(90L), StatusParticipacao.CONVIDADO)
                .com(usuario(91L), StatusParticipacao.LISTA_DE_ESPERA);

        // 4 confirmados + 2 de fora: pedir times de 3 precisaria de 6 confirmados
        assertThatThrownBy(() -> service.sortear(PELADA_ID, porJogadores(3), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("a pelada tem 4");
    }

    @Test
    @DisplayName("divisão que fecha vai para o balanceador com os números resolvidos")
    void divisaoQueFechaSegue() {
        peladaCom(11, StatusPelada.AGENDADA);
        when(balanceador.balancear(anyList(), eq(2), eq(5), any(Random.class)))
                .thenReturn(new TimesBalanceados(List.of(List.of(), List.of()), List.of()));

        // 11 confirmados em 2 times: 5 por time, e o 11º vira reserva
        assertThatCode(() -> service.sortear(PELADA_ID, porTimes(2), ORGANIZADOR_ID))
                .doesNotThrowAnyException();

        verify(balanceador).balancear(anyList(), eq(2), eq(5), any(Random.class));
    }

    @Test
    @DisplayName("a mesma semente é repassada ao balanceador, para repetir o sorteio")
    void sementeInformadaEUsada() {
        peladaCom(10, StatusPelada.AGENDADA);
        when(balanceador.balancear(anyList(), eq(2), eq(5), any(Random.class)))
                .thenReturn(new TimesBalanceados(List.of(List.of(), List.of()), List.of()));

        service.sortear(PELADA_ID, new CriterioSorteio(2, null, 4242L), ORGANIZADOR_ID);

        verify(sorteioMapper).toResponse(any(Pelada.class), any(TimesBalanceados.class),
                eq(5), eq(10), eq(4242L));
    }

    /* ------------------------------------------------------------------ */

    private static CriterioSorteio porTimes(int quantidade) {
        return new CriterioSorteio(quantidade, null, null);
    }

    private static CriterioSorteio porJogadores(int quantidade) {
        return new CriterioSorteio(null, quantidade, null);
    }

    /**
     * @param confirmados total de confirmados, contando o organizador
     */
    private CenarioDePelada peladaCom(int confirmados, StatusPelada status) {
        CenarioDePelada cenario = CenarioDePelada.pelada(20, status).comConfirmados(confirmados - 1);
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(cenario.pelada());
        return cenario;
    }
}
