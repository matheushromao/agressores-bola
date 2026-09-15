package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.EstatisticaRequest;
import com.hmz.agressores_da_bola.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.EstatisticaMapper;
import com.hmz.agressores_da_bola.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.repository.EstatisticaPartidaRepository;
import com.hmz.agressores_da_bola.repository.ParticipacaoPeladaRepository;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras da súmula: quem pode lançar, quando, e quais números valem para
 * cada posição jogada.
 */
@ExtendWith(MockitoExtension.class)
class EstatisticaServiceImplTest {

    private static final Long PELADA_ID = 10L;
    private static final Long ORGANIZADOR_ID = 1L;
    private static final Long JOGADOR_ID = 2L;

    @Mock private EstatisticaPartidaRepository estatisticaRepository;
    @Mock private ParticipacaoPeladaRepository participacaoRepository;
    @Mock private PeladaRepository peladaRepository;
    @Mock private EstatisticaMapper estatisticaMapper;

    private EstatisticaServiceImpl service;

    @BeforeEach
    void criarService() {
        service = new EstatisticaServiceImpl(
                estatisticaRepository, participacaoRepository, peladaRepository, estatisticaMapper);
    }

    @Test
    @DisplayName("só o organizador lança ou apaga a súmula")
    void naoOrganizadorNaoLancaNemApaga() {
        participacaoNa(StatusPelada.EM_ANDAMENTO, Posicao.ALA, StatusParticipacao.CONFIRMADO);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, gols(2), 3L))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.remover(PELADA_ID, JOGADOR_ID, 3L))
                .isInstanceOf(AcessoNegadoException.class);

        verify(estatisticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("defesa não vale para quem jogou na linha")
    void defesaSoParaGoleiro() {
        participacaoNa(StatusPelada.EM_ANDAMENTO, Posicao.ALA, StatusParticipacao.CONFIRMADO);
        EstatisticaRequest defesas = new EstatisticaRequest(0, 0, 0, 3, 1, null);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, defesas, ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("só vale para quem jogou no gol");
    }

    @Test
    @DisplayName("desarme não conta para goleiro")
    void desarmeSoParaLinha() {
        participacaoNa(StatusPelada.EM_ANDAMENTO, Posicao.ALA, StatusParticipacao.CONFIRMADO);
        EstatisticaRequest desarmeNoGol = new EstatisticaRequest(0, 0, 2, 0, 0, Posicao.GOLEIRO);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, desarmeNoGol, ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não é contabilizado para goleiros");
    }

    @Test
    @DisplayName("sem posicaoJogada, vale a posição do cadastro do jogador")
    void posicaoJogadaHerdaDoCadastro() {
        participacaoNa(StatusPelada.FINALIZADA, Posicao.GOLEIRO, StatusParticipacao.CONFIRMADO);
        when(estatisticaRepository.findByParticipacaoId(any())).thenReturn(Optional.empty());
        EstatisticaRequest defesas = new EstatisticaRequest(0, 0, 0, 6, 2, null);

        service.registrar(PELADA_ID, JOGADOR_ID, defesas, ORGANIZADOR_ID);

        ArgumentCaptor<EstatisticaPartida> salva = ArgumentCaptor.forClass(EstatisticaPartida.class);
        verify(estatisticaRepository).save(salva.capture());
        assertThat(salva.getValue().getPosicaoJogada()).isEqualTo(Posicao.GOLEIRO);
        assertThat(salva.getValue().getDefesas()).isEqualTo(6);
        assertThat(salva.getValue().getDefesasDificeis()).isEqualTo(2);
    }

    @Test
    @DisplayName("pelada que ainda não começou não tem súmula")
    void peladaAgendadaNaoTemSumula() {
        participacaoNa(StatusPelada.AGENDADA, Posicao.ALA, StatusParticipacao.CONFIRMADO);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, gols(1), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("em andamento ou finalizada");
    }

    @Test
    @DisplayName("quem não estava confirmado não tem estatística")
    void soConfirmadoTemEstatistica() {
        participacaoNa(StatusPelada.EM_ANDAMENTO, Posicao.ALA, StatusParticipacao.LISTA_DE_ESPERA);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, gols(1), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não estava confirmado");
    }

    /* ------------------------------------------------------------------ */

    private void participacaoNa(StatusPelada statusPelada, Posicao posicaoDoCadastro,
                                StatusParticipacao statusParticipacao) {
        Usuario organizador = Usuario.builder().id(ORGANIZADOR_ID).nickname("org").posicao(Posicao.FIXO).build();
        Usuario jogador = Usuario.builder().id(JOGADOR_ID).nickname("jogador").posicao(posicaoDoCadastro).build();

        Pelada pelada = Pelada.builder()
                .id(PELADA_ID)
                .status(statusPelada)
                .organizador(organizador)
                .maxParticipantes(10)
                .build();

        ParticipacaoPelada participacao = ParticipacaoPelada.builder()
                .id(99L)
                .usuario(jogador)
                .status(statusParticipacao)
                .build();
        pelada.adicionarParticipacao(participacao);

        when(participacaoRepository.findByPeladaIdAndUsuarioId(PELADA_ID, JOGADOR_ID))
                .thenReturn(Optional.of(participacao));
    }

    private static EstatisticaRequest gols(int quantidade) {
        return new EstatisticaRequest(quantidade, 0, 0, 0, 0, null);
    }
}
