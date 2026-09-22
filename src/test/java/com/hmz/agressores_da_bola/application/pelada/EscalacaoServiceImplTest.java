package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.port.ParticipacaoRepository;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.model.CenarioDePelada;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.AGORA;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.PELADA_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.RELOGIO;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.peladaAgendada;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestração da escalação e a posse de cada participação. Vagas e lista de
 * espera são regras da pelada, cobertas em {@code PeladaTest}.
 */
@ExtendWith(MockitoExtension.class)
class EscalacaoServiceImplTest {

    @Mock private PeladaRepository peladaRepository;
    @Mock private ParticipacaoRepository participacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PeladaMapper peladaMapper;

    private EscalacaoServiceImpl service;

    @BeforeEach
    void criarService() {
        service = new EscalacaoServiceImpl(
                peladaRepository, participacaoRepository, usuarioRepository, peladaMapper, RELOGIO);
    }

    @Test
    @DisplayName("o jogador entra sozinho, com a inscrição datada pelo relógio, e é gravado")
    void jogadorEntraSozinho() {
        Pelada pelada = peladaAgendada(10).pelada();
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(pelada);
        when(usuarioRepository.obter(2L)).thenReturn(usuario(2L));

        service.adicionar(PELADA_ID, 2L, StatusParticipacao.CONFIRMADO, 2L);

        ArgumentCaptor<ParticipacaoPelada> gravada = ArgumentCaptor.forClass(ParticipacaoPelada.class);
        verify(participacaoRepository).salvar(gravada.capture());
        assertThat(gravada.getValue().pertenceAoUsuario(2L)).isTrue();
        assertThat(gravada.getValue().getDataInscricao()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("o jogador não inclui outra pessoa, e o usuário alvo nem é buscado")
    void jogadorNaoIncluiOutro() {
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(peladaAgendada(10).pelada());

        assertThatThrownBy(() -> service.adicionar(PELADA_ID, 3L, null, 2L))
                .isInstanceOf(AcessoNegadoException.class);

        verify(usuarioRepository, never()).obter(any());
        verify(participacaoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("um jogador não altera nem remove a participação de outro")
    void jogadorNaoMexeEmOutro() {
        CenarioDePelada cenario = peladaAgendada(10).com(usuario(2L), StatusParticipacao.CONFIRMADO);
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(cenario.pelada());

        assertThatThrownBy(() -> service.alterarStatus(PELADA_ID, 2L, StatusParticipacao.RECUSADO, 3L))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.remover(PELADA_ID, 2L, 3L))
                .isInstanceOf(AcessoNegadoException.class);

        assertThat(cenario.participacaoDe(2L).getStatus()).isEqualTo(StatusParticipacao.CONFIRMADO);
        verify(peladaRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("o próprio jogador sai e a pelada é gravada com a vaga já repassada")
    void jogadorSaiEVagaERepassada() {
        CenarioDePelada cenario = peladaAgendada(2)
                .com(usuario(2L), StatusParticipacao.CONFIRMADO)
                .com(usuario(3L), StatusParticipacao.LISTA_DE_ESPERA);
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(cenario.pelada());

        service.remover(PELADA_ID, 2L, 2L);

        verify(peladaRepository).salvar(cenario.pelada());
        assertThat(cenario.participacaoDe(3L).getStatus()).isEqualTo(StatusParticipacao.CONFIRMADO);
    }
}
