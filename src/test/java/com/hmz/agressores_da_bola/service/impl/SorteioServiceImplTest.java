package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.SorteioRequest;
import com.hmz.agressores_da_bola.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.SorteioMapper;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import com.hmz.agressores_da_bola.service.sorteio.BalanceadorDeTimes;
import com.hmz.agressores_da_bola.service.sorteio.TimesBalanceados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
 * Regras do sorteio: quem pode pedir, quando, e o que a API responde quando a
 * divisão não fecha — a mensagem é o que o organizador lê na tela.
 */
@ExtendWith(MockitoExtension.class)
class SorteioServiceImplTest {

    private static final Long PELADA_ID = 10L;
    private static final Long ORGANIZADOR_ID = 1L;

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
        peladaCom(10);

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

    /**
     * O caso que motivou a correção: a mensagem antiga falava do mínimo
     * absoluto do sorteio ("pelo menos 4 para formar 2 times de 2"), que é
     * verdade e não ajuda em nada quem pediu times de 6.
     */
    @Test
    @DisplayName("faltando gente para o tamanho de time pedido, a mensagem diz quanto falta")
    void mensagemCitaOTamanhoDeTimePedido() {
        peladaCom(10);

        assertThatThrownBy(() -> service.sortear(PELADA_ID, porJogadores(6), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("times de 6")
                .hasMessageContaining("pelo menos 12")
                .hasMessageContaining("a pelada tem 10");
    }

    @Test
    @DisplayName("faltando gente para a quantidade de times pedida, a mensagem diz quanto falta")
    void mensagemCitaAQuantidadeDeTimesPedida() {
        peladaCom(10);

        assertThatThrownBy(() -> service.sortear(PELADA_ID, porTimes(6), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("6 times")
                .hasMessageContaining("pelo menos 12")
                .hasMessageContaining("a pelada tem 10");
    }

    @Test
    @DisplayName("convidado e lista de espera não entram na conta")
    void soConfirmadosEntram() {
        Pelada pelada = peladaCom(4);
        pelada.getParticipacoes().add(participacao(pelada, 90L, StatusParticipacao.CONVIDADO));
        pelada.getParticipacoes().add(participacao(pelada, 91L, StatusParticipacao.LISTA_DE_ESPERA));

        // 4 confirmados + 2 de fora: pedir times de 3 precisaria de 6 confirmados
        assertThatThrownBy(() -> service.sortear(PELADA_ID, porJogadores(3), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("a pelada tem 4");
    }

    @Test
    @DisplayName("divisão que fecha vai para o balanceador com os números resolvidos")
    void divisaoQueFechaSegue() {
        peladaCom(11);
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
        peladaCom(10);
        when(balanceador.balancear(anyList(), eq(2), eq(5), any(Random.class)))
                .thenReturn(new TimesBalanceados(List.of(List.of(), List.of()), List.of()));

        service.sortear(PELADA_ID, new SorteioRequest(2, null, 4242L), ORGANIZADOR_ID);

        verify(sorteioMapper).toResponse(any(Pelada.class), any(TimesBalanceados.class),
                eq(5), eq(10), eq(4242L));
    }

    /* ------------------------------------------------------------------
     * Fixtures
     * ------------------------------------------------------------------ */

    private static SorteioRequest porTimes(int quantidade) {
        return new SorteioRequest(quantidade, null, null);
    }

    private static SorteioRequest porJogadores(int quantidade) {
        return new SorteioRequest(null, quantidade, null);
    }

    private Pelada peladaCom(int confirmados) {
        return peladaCom(confirmados, StatusPelada.AGENDADA);
    }

    private Pelada peladaCom(int confirmados, StatusPelada status) {
        Usuario organizador = usuario(ORGANIZADOR_ID);
        Pelada pelada = Pelada.builder()
                .id(PELADA_ID)
                .organizador(organizador)
                .status(status)
                .maxParticipantes(20)
                .participacoes(new ArrayList<>())
                .build();

        for (int i = 0; i < confirmados; i++) {
            pelada.getParticipacoes().add(
                    participacao(pelada, 100L + i, StatusParticipacao.CONFIRMADO));
        }

        when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));
        return pelada;
    }

    private static ParticipacaoPelada participacao(Pelada pelada, Long usuarioId, StatusParticipacao status) {
        return ParticipacaoPelada.builder()
                .pelada(pelada)
                .usuario(usuario(usuarioId))
                .status(status)
                .build();
    }

    private static Usuario usuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nickname("jogador" + id)
                .posicao(Posicao.ALA)
                .estrelas(new BigDecimal("3.0"))
                .build();
    }
}
