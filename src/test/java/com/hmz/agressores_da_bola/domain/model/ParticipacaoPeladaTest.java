package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.hmz.agressores_da_bola.domain.model.CenarioDePelada.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras da súmula: quando se lança, para quem, e quais números valem em
 * cada posição jogada.
 */
class ParticipacaoPeladaTest {

    private static final Long JOGADOR_ID = 2L;

    @Test
    @DisplayName("defesa não vale para quem jogou na linha")
    void defesaSoParaGoleiro() {
        ParticipacaoPelada participacao = participacao(StatusPelada.EM_ANDAMENTO, Posicao.ALA);

        assertThatThrownBy(() -> participacao.lancarEstatistica(null, numeros(0, 0, 0, 3, 1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("só vale para quem jogou no gol");
        assertThat(participacao.getEstatistica()).isNull();
    }

    @Test
    @DisplayName("desarme não conta para goleiro")
    void desarmeSoParaLinha() {
        ParticipacaoPelada participacao = participacao(StatusPelada.EM_ANDAMENTO, Posicao.ALA);

        assertThatThrownBy(() -> participacao.lancarEstatistica(Posicao.GOLEIRO, numeros(0, 0, 2, 0, 0)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não é contabilizado para goleiros");
    }

    @Test
    @DisplayName("sem posição informada, vale a do cadastro do jogador")
    void posicaoJogadaHerdaDoCadastro() {
        ParticipacaoPelada participacao = participacao(StatusPelada.FINALIZADA, Posicao.GOLEIRO);

        EstatisticaPartida sumula = participacao.lancarEstatistica(null, numeros(0, 0, 0, 6, 2));

        assertThat(sumula.getPosicaoJogada()).isEqualTo(Posicao.GOLEIRO);
        assertThat(sumula.getDefesas()).isEqualTo(6);
        assertThat(sumula.getDefesasDificeis()).isEqualTo(2);
        assertThat(sumula.getParticipacao()).isSameAs(participacao);
    }

    @Test
    @DisplayName("lançar de novo corrige a mesma súmula em vez de criar outra")
    void relancarCorrige() {
        ParticipacaoPelada participacao = participacao(StatusPelada.EM_ANDAMENTO, Posicao.PIVO);

        EstatisticaPartida primeira = participacao.lancarEstatistica(null, numeros(1, 0, 0, 0, 0));
        EstatisticaPartida segunda = participacao.lancarEstatistica(null, numeros(3, 1, 0, 0, 0));

        assertThat(segunda).isSameAs(primeira);
        assertThat(segunda.getGols()).isEqualTo(3);
        assertThat(segunda.pontuacao()).isEqualTo(37);
    }

    @Test
    @DisplayName("pelada que ainda não começou não tem súmula")
    void peladaAgendadaNaoTemSumula() {
        ParticipacaoPelada participacao = participacao(StatusPelada.AGENDADA, Posicao.ALA);

        assertThatThrownBy(() -> participacao.lancarEstatistica(null, numeros(1, 0, 0, 0, 0)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("em andamento ou finalizada");
    }

    @Test
    @DisplayName("quem não estava confirmado não tem estatística")
    void soConfirmadoTemEstatistica() {
        ParticipacaoPelada participacao = CenarioDePelada.pelada(10, StatusPelada.EM_ANDAMENTO)
                .com(usuario(JOGADOR_ID), StatusParticipacao.LISTA_DE_ESPERA)
                .participacaoDe(JOGADOR_ID);

        assertThatThrownBy(() -> participacao.lancarEstatistica(null, numeros(1, 0, 0, 0, 0)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não estava confirmado");
    }

    @Test
    @DisplayName("apagar uma súmula que não existe é recurso não encontrado")
    void removerSemSumula() {
        ParticipacaoPelada participacao = participacao(StatusPelada.FINALIZADA, Posicao.ALA);

        assertThatThrownBy(participacao::removerEstatistica)
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O jogador de id 2 não possui súmula lançada na pelada de id 10");
    }

    /* ------------------------------------------------------------------ */

    private static ParticipacaoPelada participacao(StatusPelada statusPelada, Posicao posicaoDoCadastro) {
        return CenarioDePelada.pelada(10, statusPelada)
                .com(usuario(JOGADOR_ID, posicaoDoCadastro), StatusParticipacao.CONFIRMADO)
                .participacaoDe(JOGADOR_ID);
    }

    private static ResumoEstatistico numeros(int gols, int assistencias, int desarmes,
                                             int defesas, int defesasDificeis) {
        return new ResumoEstatistico(gols, assistencias, desarmes, defesas, defesasDificeis);
    }
}
