package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.AGORA;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.ORGANIZADOR_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.PELADA_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.RELOGIO;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.dadosEm;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.dadosValidos;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.peladaAgendada;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Orquestração do agendamento, com as portas simuladas: quem vira
 * organizador, quando a agenda é consultada e que nada é gravado sem posse.
 * As regras da pelada em si estão em {@code PeladaTest}.
 */
@ExtendWith(MockitoExtension.class)
class PeladaServiceImplTest {

    @Mock private PeladaRepository peladaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PeladaMapper peladaMapper;

    private PeladaServiceImpl service;

    @BeforeEach
    void criarService() {
        service = new PeladaServiceImpl(peladaRepository, usuarioRepository, peladaMapper, RELOGIO);
    }

    @Test
    @DisplayName("a pelada criada tem o usuário logado como organizador, já confirmado")
    void criarUsaOLogadoComoOrganizador() {
        when(usuarioRepository.obter(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID));

        service.criar(dadosValidos(), ORGANIZADOR_ID);

        ArgumentCaptor<Pelada> salva = ArgumentCaptor.forClass(Pelada.class);
        verify(peladaRepository).salvar(salva.capture());
        assertThat(salva.getValue().organizadaPor(ORGANIZADOR_ID)).isTrue();
        assertThat(salva.getValue().buscarParticipacaoDoUsuario(ORGANIZADOR_ID)).get()
                .extracting(ParticipacaoPelada::getStatus)
                .isEqualTo(StatusParticipacao.CONFIRMADO);
    }

    @Test
    @DisplayName("o horário de início é comparado com o relógio injetado")
    void inicioComparadoComORelogio() {
        when(usuarioRepository.obter(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID));

        assertThatThrownBy(() -> service.criar(
                dadosEm(AGORA.toLocalDate(), AGORA.toLocalTime().minusHours(1), 10), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(peladaRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("mudar o horário consulta a agenda do organizador antes de gravar")
    void mudarHorarioConsultaAgenda() {
        Pelada pelada = peladaAgendada(10).pelada();
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(pelada);
        when(peladaRepository.organizadorTemPeladaEm(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(
                PELADA_ID, dadosEm(pelada.getData(), LocalTime.of(20, 0), 10), ORGANIZADOR_ID))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já possui uma pelada marcada");

        verify(peladaRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("quem não organiza não atualiza, não muda o status e não apaga a pelada")
    void naoOrganizadorNaoAltera() {
        Pelada pelada = peladaAgendada(10).pelada();
        when(peladaRepository.obterComParticipantes(PELADA_ID)).thenReturn(pelada);
        when(peladaRepository.obter(PELADA_ID)).thenReturn(pelada);

        assertThatThrownBy(() -> service.atualizar(PELADA_ID, dadosValidos(), 2L))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.alterarStatus(PELADA_ID, StatusPelada.CANCELADA, 2L))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.deletar(PELADA_ID, 2L))
                .isInstanceOf(AcessoNegadoException.class);

        verify(peladaRepository, never()).salvar(any());
        verify(peladaRepository, never()).remover(any());
    }
}
