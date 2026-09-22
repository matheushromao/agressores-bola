package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.AGORA;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.ORGANIZADOR_ID;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.dadosEm;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.dadosValidos;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.peladaAgendada;
import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As regras da pelada testadas direto na entidade: sem Spring, sem banco e
 * sem mocks — é o ganho de o domínio proteger as próprias invariantes.
 */
class PeladaTest {

    private static final AgendaDePeladas AGENDA_LIVRE = (organizador, data, hora) -> false;
    private static final AgendaDePeladas AGENDA_OCUPADA = (organizador, data, hora) -> true;

    private final Usuario jogador = usuario(2L);
    private final Usuario outro = usuario(3L);

    @Nested
    @DisplayName("Agendamento")
    class Agendamento {

        @Test
        @DisplayName("nasce agendada, com o organizador já confirmado e a UF em maiúsculas")
        void organizadorJaConfirmado() {
            Pelada pelada = Pelada.agendar(dadosValidos(), usuario(ORGANIZADOR_ID), AGORA, AGENDA_LIVRE);

            assertThat(pelada.getStatus()).isEqualTo(StatusPelada.AGENDADA);
            assertThat(pelada.organizadaPor(ORGANIZADOR_ID)).isTrue();
            assertThat(pelada.totalConfirmados()).isEqualTo(1);
            assertThat(pelada.getEstado()).isEqualTo("SP");
            assertThat(pelada.getParticipacoes().getFirst().getDataInscricao()).isEqualTo(AGORA);
        }

        @Test
        @DisplayName("não agenda com o horário de início já passado")
        void inicioNoPassado() {
            DadosPelada hojeCedo = dadosEm(AGORA.toLocalDate(), LocalTime.of(8, 0), 10);

            assertThatThrownBy(() -> Pelada.agendar(hojeCedo, usuario(ORGANIZADOR_ID), AGORA, AGENDA_LIVRE))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já passou");
        }

        @Test
        @DisplayName("não agenda duas peladas do mesmo organizador no mesmo horário")
        void agendaOcupada() {
            assertThatThrownBy(() -> Pelada.agendar(dadosValidos(), usuario(ORGANIZADOR_ID), AGORA, AGENDA_OCUPADA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já possui uma pelada marcada");
        }

        @Test
        @DisplayName("mantendo data e hora, a atualização nem consulta a agenda")
        void atualizarSemMudarAgenda() {
            Pelada pelada = peladaAgendada(10).pelada();

            pelada.atualizar(dadosEm(pelada.getData(), pelada.getHoraInicio(), 12), AGORA, AGENDA_OCUPADA);

            assertThat(pelada.getMaxParticipantes()).isEqualTo(12);
        }

        @Test
        @DisplayName("o limite de vagas não pode ficar abaixo dos já confirmados")
        void limiteAbaixoDosConfirmados() {
            Pelada pelada = peladaAgendada(10).comConfirmados(3).pelada();

            assertThatThrownBy(() -> pelada.atualizar(
                    dadosEm(pelada.getData(), pelada.getHoraInicio(), 3), AGORA, AGENDA_LIVRE))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já possui 4 jogadores confirmados");
        }
    }

    @Nested
    @DisplayName("Situação")
    class Situacao {

        @Test
        @DisplayName("pelada encerrada não muda mais de status nem aceita alterações")
        void encerradaNaoMuda() {
            Pelada pelada = CenarioDePelada.pelada(10, StatusPelada.FINALIZADA).pelada();

            assertThatThrownBy(() -> pelada.alterarStatus(StatusPelada.AGENDADA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("A pelada está finalizada e o status não pode mais ser alterado");
            assertThatThrownBy(() -> pelada.escalar(jogador, StatusParticipacao.CONFIRMADO, AGORA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("A pelada está finalizada e não aceita mais alterações");
        }

        @Test
        @DisplayName("confirmar exige pelo menos 2 jogadores confirmados")
        void confirmarExigeDoisConfirmados() {
            Pelada sozinha = peladaAgendada(10).pelada();
            Pelada comDois = peladaAgendada(10).com(jogador, StatusParticipacao.CONFIRMADO).pelada();

            assertThatThrownBy(() -> sozinha.alterarStatus(StatusPelada.CONFIRMADA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("2 jogadores");

            comDois.alterarStatus(StatusPelada.CONFIRMADA);
            assertThat(comDois.getStatus()).isEqualTo(StatusPelada.CONFIRMADA);
        }
    }

    @Nested
    @DisplayName("Escalação")
    class Escalacao {

        @Test
        @DisplayName("pelada lotada recusa confirmado e aceita lista de espera")
        void lotadaSoAceitaListaDeEspera() {
            Pelada pelada = peladaAgendada(2).com(jogador, StatusParticipacao.CONFIRMADO).pelada();

            assertThatThrownBy(() -> pelada.escalar(outro, StatusParticipacao.CONFIRMADO, AGORA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("lista de espera");

            ParticipacaoPelada naEspera = pelada.escalar(outro, StatusParticipacao.LISTA_DE_ESPERA, AGORA);
            assertThat(naEspera.getStatus()).isEqualTo(StatusParticipacao.LISTA_DE_ESPERA);
            assertThat(naEspera.getPelada()).isSameAs(pelada);
        }

        @Test
        @DisplayName("o mesmo jogador não entra duas vezes")
        void semDuplicidade() {
            Pelada pelada = peladaAgendada(10).com(jogador, StatusParticipacao.CONVIDADO).pelada();

            assertThatThrownBy(() -> pelada.escalar(jogador, StatusParticipacao.CONFIRMADO, AGORA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já faz parte desta pelada");
        }

        @Test
        @DisplayName("quando um confirmado sai, o primeiro da lista de espera é promovido")
        void saidaPromoveListaDeEspera() {
            Usuario terceiro = usuario(4L);
            CenarioDePelada cenario = peladaAgendada(2)
                    .com(jogador, StatusParticipacao.CONFIRMADO)
                    .com(outro, StatusParticipacao.LISTA_DE_ESPERA)
                    .com(terceiro, StatusParticipacao.LISTA_DE_ESPERA);

            cenario.pelada().removerParticipante(jogador.getId());

            assertThat(cenario.pelada().buscarParticipacaoDoUsuario(jogador.getId())).isEmpty();
            assertThat(cenario.participacaoDe(outro.getId()).getStatus()).isEqualTo(StatusParticipacao.CONFIRMADO);
            assertThat(cenario.participacaoDe(terceiro.getId()).getStatus())
                    .isEqualTo(StatusParticipacao.LISTA_DE_ESPERA);
        }

        @Test
        @DisplayName("recusar a presença também libera a vaga para a lista de espera")
        void recusaPromoveListaDeEspera() {
            CenarioDePelada cenario = peladaAgendada(2)
                    .com(jogador, StatusParticipacao.CONFIRMADO)
                    .com(outro, StatusParticipacao.LISTA_DE_ESPERA);

            cenario.pelada().alterarParticipacao(jogador.getId(), StatusParticipacao.RECUSADO);

            assertThat(cenario.participacaoDe(jogador.getId()).getStatus()).isEqualTo(StatusParticipacao.RECUSADO);
            assertThat(cenario.participacaoDe(outro.getId()).getStatus()).isEqualTo(StatusParticipacao.CONFIRMADO);
        }

        @Test
        @DisplayName("o organizador não sai nem é removido da própria pelada")
        void organizadorNaoSai() {
            Pelada pelada = peladaAgendada(10).pelada();

            assertThatThrownBy(() -> pelada.alterarParticipacao(ORGANIZADOR_ID, StatusParticipacao.RECUSADO))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> pelada.removerParticipante(ORGANIZADOR_ID))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Posse")
    class Posse {

        @Test
        @DisplayName("só o organizador passa pela checagem de organizador, com a ação na mensagem")
        void exigirOrganizador() {
            Pelada pelada = peladaAgendada(10).pelada();

            pelada.exigirOrganizador(ORGANIZADOR_ID, "alterar esta pelada");
            assertThatThrownBy(() -> pelada.exigirOrganizador(2L, "alterar esta pelada"))
                    .isInstanceOf(AcessoNegadoException.class)
                    .hasMessage("Só o organizador pode alterar esta pelada");
        }

        @Test
        @DisplayName("o jogador escala a si mesmo, mas só o organizador escala outra pessoa")
        void permissaoParaEscalar() {
            Pelada pelada = peladaAgendada(10).pelada();

            pelada.exigirPermissaoParaEscalar(2L, 2L);
            pelada.exigirPermissaoParaEscalar(3L, ORGANIZADOR_ID);
            assertThatThrownBy(() -> pelada.exigirPermissaoParaEscalar(3L, 2L))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }
}
