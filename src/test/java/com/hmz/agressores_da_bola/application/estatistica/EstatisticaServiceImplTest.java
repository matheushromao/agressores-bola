package com.hmz.agressores_da_bola.application.estatistica;

import com.hmz.agressores_da_bola.application.port.EstatisticaRepository;
import com.hmz.agressores_da_bola.application.port.ParticipacaoRepository;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.CenarioDePelada;
import com.hmz.agressores_da_bola.domain.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.domain.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.ORGANIZADOR_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.PELADA_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestração da súmula: quem pode lançar e o que é gravado. Quais números
 * valem em cada posição está em {@code ParticipacaoPeladaTest}.
 */
@ExtendWith(MockitoExtension.class)
class EstatisticaServiceImplTest {

    private static final Long JOGADOR_ID = 2L;

    @Mock private EstatisticaRepository estatisticaRepository;
    @Mock private ParticipacaoRepository participacaoRepository;
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
        participacaoNa(StatusPelada.EM_ANDAMENTO, Posicao.ALA);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, gols(2), 3L))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Só o organizador pode lançar ou apagar a súmula desta pelada");
        assertThatThrownBy(() -> service.remover(PELADA_ID, JOGADOR_ID, 3L))
                .isInstanceOf(AcessoNegadoException.class);

        verify(estatisticaRepository, never()).salvar(any());
        verify(participacaoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("o organizador lança e a súmula é gravada com a posição do cadastro")
    void organizadorLanca() {
        participacaoNa(StatusPelada.FINALIZADA, Posicao.GOLEIRO);
        LancamentoEstatistica defesas = new LancamentoEstatistica(new ResumoEstatistico(0, 0, 0, 6, 2), null);

        service.registrar(PELADA_ID, JOGADOR_ID, defesas, ORGANIZADOR_ID);

        ArgumentCaptor<EstatisticaPartida> salva = ArgumentCaptor.forClass(EstatisticaPartida.class);
        verify(estatisticaRepository).salvar(salva.capture());
        assertThat(salva.getValue().getPosicaoJogada()).isEqualTo(Posicao.GOLEIRO);
        assertThat(salva.getValue().getDefesas()).isEqualTo(6);
    }

    @Test
    @DisplayName("lançamento recusado pela regra da posição não grava nada")
    void lancamentoRecusadoNaoGrava() {
        participacaoNa(StatusPelada.EM_ANDAMENTO, Posicao.ALA);
        LancamentoEstatistica defesasNaLinha =
                new LancamentoEstatistica(new ResumoEstatistico(0, 0, 0, 3, 1), null);

        assertThatThrownBy(() -> service.registrar(PELADA_ID, JOGADOR_ID, defesasNaLinha, ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(estatisticaRepository, never()).salvar(any());
    }

    /* ------------------------------------------------------------------ */

    private void participacaoNa(StatusPelada statusPelada, Posicao posicaoDoCadastro) {
        ParticipacaoPelada participacao = CenarioDePelada.pelada(10, statusPelada)
                .com(usuario(JOGADOR_ID, posicaoDoCadastro), StatusParticipacao.CONFIRMADO)
                .participacaoDe(JOGADOR_ID);

        when(participacaoRepository.obter(PELADA_ID, JOGADOR_ID)).thenReturn(participacao);
    }

    private static LancamentoEstatistica gols(int quantidade) {
        return new LancamentoEstatistica(new ResumoEstatistico(quantidade, 0, 0, 0, 0), null);
    }
}
