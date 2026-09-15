package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.ParticipacaoRequest;
import com.hmz.agressores_da_bola.dto.PeladaRequest;
import com.hmz.agressores_da_bola.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.PeladaMapper;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;
import com.hmz.agressores_da_bola.repository.ParticipacaoPeladaRepository;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import com.hmz.agressores_da_bola.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de escalação e de posse da pelada, sem banco: os repositórios são
 * simulados e o estado é conferido direto na entidade.
 */
@ExtendWith(MockitoExtension.class)
class PeladaServiceImplTest {

    private static final Long PELADA_ID = 10L;

    @Mock private PeladaRepository peladaRepository;
    @Mock private ParticipacaoPeladaRepository participacaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PeladaMapper peladaMapper;

    private PeladaServiceImpl service;

    private final Usuario organizador = usuario(1L);
    private final Usuario jogador = usuario(2L);
    private final Usuario outro = usuario(3L);

    @BeforeEach
    void criarService() {
        service = new PeladaServiceImpl(peladaRepository, participacaoRepository, usuarioRepository, peladaMapper);
    }

    @Nested
    @DisplayName("Posse da pelada")
    class Posse {

        @Test
        @DisplayName("a pelada criada tem o usuário logado como organizador, já confirmado")
        void criarUsaOLogadoComoOrganizador() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(organizador));
            when(peladaMapper.toEntity(any(), eq(organizador))).thenAnswer(invocacao ->
                    Pelada.builder().organizador(organizador).maxParticipantes(10)
                            .status(StatusPelada.AGENDADA).build());

            service.criar(requestValido(), 1L);

            ArgumentCaptor<Pelada> salva = ArgumentCaptor.forClass(Pelada.class);
            verify(peladaRepository).save(salva.capture());
            assertThat(salva.getValue().organizadaPor(1L)).isTrue();
            assertThat(salva.getValue().buscarParticipacaoDoUsuario(1L))
                    .get()
                    .extracting(ParticipacaoPelada::getStatus)
                    .isEqualTo(StatusParticipacao.CONFIRMADO);
        }

        @Test
        @DisplayName("quem não organiza não atualiza, não muda o status e não apaga a pelada")
        void naoOrganizadorNaoAltera() {
            Pelada pelada = pelada(10, StatusPelada.AGENDADA);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));
            when(peladaRepository.findById(PELADA_ID)).thenReturn(Optional.of(pelada));

            assertThatThrownBy(() -> service.atualizar(PELADA_ID, requestValido(), 2L))
                    .isInstanceOf(AcessoNegadoException.class);
            assertThatThrownBy(() -> service.alterarStatus(PELADA_ID, StatusPelada.CANCELADA, 2L))
                    .isInstanceOf(AcessoNegadoException.class);
            assertThatThrownBy(() -> service.deletar(PELADA_ID, 2L))
                    .isInstanceOf(AcessoNegadoException.class);

            verify(peladaRepository, never()).save(any());
            // any(Pelada.class): o Spring Data 4 também tem delete(DeleteSpecification)
            verify(peladaRepository, never()).delete(any(Pelada.class));
        }
    }

    @Nested
    @DisplayName("Escalação")
    class Escalacao {

        @Test
        @DisplayName("o jogador entra sozinho, mas não inclui outra pessoa")
        void jogadorSoIncluiASiMesmo() {
            Pelada pelada = pelada(10, StatusPelada.AGENDADA);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(jogador));

            service.adicionarParticipante(PELADA_ID, new ParticipacaoRequest(2L, null), 2L);
            assertThat(pelada.buscarParticipacaoDoUsuario(2L)).isPresent();

            assertThatThrownBy(() ->
                    service.adicionarParticipante(PELADA_ID, new ParticipacaoRequest(3L, null), 2L))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        @DisplayName("pelada lotada recusa confirmado e aceita lista de espera")
        void lotadaSoAceitaListaDeEspera() {
            Pelada pelada = pelada(2, StatusPelada.AGENDADA);
            participar(pelada, jogador, StatusParticipacao.CONFIRMADO, 1);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(outro));

            assertThatThrownBy(() -> service.adicionarParticipante(
                    PELADA_ID, new ParticipacaoRequest(3L, StatusParticipacao.CONFIRMADO), 3L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("lista de espera");

            service.adicionarParticipante(
                    PELADA_ID, new ParticipacaoRequest(3L, StatusParticipacao.LISTA_DE_ESPERA), 3L);
            assertThat(pelada.buscarParticipacaoDoUsuario(3L)).get()
                    .extracting(ParticipacaoPelada::getStatus)
                    .isEqualTo(StatusParticipacao.LISTA_DE_ESPERA);
        }

        @Test
        @DisplayName("quando um confirmado sai, o primeiro da lista de espera é promovido")
        void saidaPromoveListaDeEspera() {
            Pelada pelada = pelada(2, StatusPelada.AGENDADA);
            participar(pelada, jogador, StatusParticipacao.CONFIRMADO, 1);
            participar(pelada, outro, StatusParticipacao.LISTA_DE_ESPERA, 2);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));

            service.removerParticipante(PELADA_ID, 2L, 2L);

            assertThat(pelada.buscarParticipacaoDoUsuario(2L)).isEmpty();
            assertThat(pelada.buscarParticipacaoDoUsuario(3L)).get()
                    .extracting(ParticipacaoPelada::getStatus)
                    .isEqualTo(StatusParticipacao.CONFIRMADO);
        }

        @Test
        @DisplayName("um jogador não altera nem remove a participação de outro")
        void jogadorNaoMexeEmOutro() {
            Pelada pelada = pelada(10, StatusPelada.AGENDADA);
            participar(pelada, jogador, StatusParticipacao.CONFIRMADO, 1);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));

            assertThatThrownBy(() -> service.alterarStatusParticipacao(
                    PELADA_ID, 2L, StatusParticipacao.RECUSADO, 3L))
                    .isInstanceOf(AcessoNegadoException.class);
            assertThatThrownBy(() -> service.removerParticipante(PELADA_ID, 2L, 3L))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        @DisplayName("o organizador não sai da própria pelada")
        void organizadorNaoSai() {
            Pelada pelada = pelada(10, StatusPelada.AGENDADA);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));

            assertThatThrownBy(() -> service.alterarStatusParticipacao(
                    PELADA_ID, 1L, StatusParticipacao.RECUSADO, 1L))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> service.removerParticipante(PELADA_ID, 1L, 1L))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Status da pelada")
    class Status {

        @Test
        @DisplayName("pelada encerrada não muda mais de status")
        void encerradaNaoMuda() {
            Pelada pelada = pelada(10, StatusPelada.FINALIZADA);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));

            assertThatThrownBy(() -> service.alterarStatus(PELADA_ID, StatusPelada.AGENDADA, 1L))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("confirmar exige pelo menos 2 jogadores confirmados")
        void confirmarExigeDoisConfirmados() {
            Pelada pelada = pelada(10, StatusPelada.AGENDADA);
            when(peladaRepository.buscarComParticipantes(PELADA_ID)).thenReturn(Optional.of(pelada));

            assertThatThrownBy(() -> service.alterarStatus(PELADA_ID, StatusPelada.CONFIRMADA, 1L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("2 jogadores");
        }
    }

    /* ------------------------------------------------------------------ */

    private Pelada pelada(int maxParticipantes, StatusPelada status) {
        Pelada pelada = Pelada.builder()
                .id(PELADA_ID)
                .nome("Pelada de quinta")
                .data(LocalDate.now().plusDays(1))
                .horaInicio(LocalTime.of(19, 0))
                .horaFim(LocalTime.of(21, 0))
                .maxParticipantes(maxParticipantes)
                .status(status)
                .organizador(organizador)
                .build();
        participar(pelada, organizador, StatusParticipacao.CONFIRMADO, 0);
        return pelada;
    }

    private void participar(Pelada pelada, Usuario usuario, StatusParticipacao status, int ordem) {
        pelada.adicionarParticipacao(ParticipacaoPelada.builder()
                .usuario(usuario)
                .status(status)
                .dataInscricao(LocalDateTime.now().minusHours(10).plusMinutes(ordem))
                .build());
    }

    private static Usuario usuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nickname("jogador-" + id)
                .posicao(Posicao.ALA)
                .build();
    }

    private static PeladaRequest requestValido() {
        return new PeladaRequest("Pelada de quinta", null, LocalDate.now().plusDays(1),
                LocalTime.of(19, 0), LocalTime.of(21, 0), "Arena", "Rua A, 1", "Sorocaba", "SP",
                TipoCampo.SOCIETY, 10, BigDecimal.TEN);
    }
}
