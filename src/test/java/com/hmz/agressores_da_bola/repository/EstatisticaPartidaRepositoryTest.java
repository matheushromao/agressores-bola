package com.hmz.agressores_da_bola.repository;

import com.hmz.agressores_da_bola.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;
import com.hmz.agressores_da_bola.repository.projection.TotaisJogador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercita a agregação do ranking contra o banco de verdade — é a única
 * forma de garantir que o {@code group by} e a expressão de construtor
 * realmente rodam, e não só compilam. O {@code @DataJpaTest} desfaz a
 * transação no fim, então nada sobra na base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EstatisticaPartidaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EstatisticaPartidaRepository estatisticaRepository;

    private Pelada pelada;

    @BeforeEach
    void prepararPelada() {
        Usuario organizador = persistirUsuario("org", Posicao.MEIA);
        pelada = persistirPelada(organizador);
    }

    @Test
    @DisplayName("soma as estatísticas por jogador e permite calcular a pontuação")
    void deveSomarPorJogador() {
        Usuario artilheiro = persistirUsuario("artilheiro", Posicao.ATACANTE);
        Usuario goleiro = persistirUsuario("paredao", Posicao.GOLEIRO);

        // O artilheiro joga duas peladas para provar que a soma acumula.
        persistirEstatistica(artilheiro, Posicao.ATACANTE, 2, 1, 3, 0, 0);
        persistirEstatistica(goleiro, Posicao.GOLEIRO, 0, 1, 0, 8, 3);

        Pelada outraPelada = persistirPelada(artilheiro);
        persistirEstatisticaEm(outraPelada, artilheiro, Posicao.ATACANTE, 1, 0, 2, 0, 0);

        em.flush();
        em.clear();

        List<TotaisJogador> totais = estatisticaRepository.somarPorJogador(null);

        TotaisJogador doArtilheiro = totalDe(totais, artilheiro);
        assertThat(doArtilheiro.jogos()).isEqualTo(2);
        assertThat(doArtilheiro.gols()).isEqualTo(3);
        assertThat(doArtilheiro.assistencias()).isEqualTo(1);
        assertThat(doArtilheiro.desarmes()).isEqualTo(5);

        // 3 gols x 10 + 1 assistência x 7 + 5 desarmes x 3 = 52
        assertThat(doArtilheiro.resumo().pontuacao()).isEqualTo(52);

        TotaisJogador doGoleiro = totalDe(totais, goleiro);
        assertThat(doGoleiro.defesas()).isEqualTo(8);
        assertThat(doGoleiro.defesasDificeis()).isEqualTo(3);

        // A defesa difícil pesa mais do que a defesa comum.
        assertThat(AtributoPontuacao.DEFESA_DIFICIL.getPeso())
                .isGreaterThan(AtributoPontuacao.DEFESA.getPeso());

        // 1 assistência x 7 + 8 defesas x 4 + 3 defesas difíceis x 8 = 63
        assertThat(doGoleiro.resumo().pontuacao()).isEqualTo(63);
    }

    @Test
    @DisplayName("restringe a soma a uma pelada quando o filtro é informado")
    void deveFiltrarPorPelada() {
        Usuario jogador = persistirUsuario("filtrado", Posicao.ATACANTE);
        persistirEstatistica(jogador, Posicao.ATACANTE, 2, 0, 0, 0, 0);

        Pelada outraPelada = persistirPelada(jogador);
        persistirEstatisticaEm(outraPelada, jogador, Posicao.ATACANTE, 5, 0, 0, 0, 0);

        em.flush();
        em.clear();

        TotaisJogador daPrimeira = totalDe(estatisticaRepository.somarPorJogador(pelada.getId()), jogador);

        assertThat(daPrimeira.jogos()).isEqualTo(1);
        assertThat(daPrimeira.gols()).isEqualTo(2);
    }

    @Test
    @DisplayName("apaga a súmula quando ela é solta da participação")
    void deveApagarASumulaOrfa() {
        Usuario jogador = persistirUsuario("orfao", Posicao.VOLANTE);
        persistirEstatistica(jogador, Posicao.VOLANTE, 1, 0, 4, 0, 0);
        em.flush();
        em.clear();

        ParticipacaoPelada participacao = em.getEntityManager()
                .createQuery("select p from ParticipacaoPelada p where p.usuario.id = :id",
                        ParticipacaoPelada.class)
                .setParameter("id", jogador.getId())
                .getSingleResult();

        assertThat(estatisticaRepository.findByParticipacaoId(participacao.getId())).isPresent();

        participacao.removerEstatistica();
        em.flush();
        em.clear();

        assertThat(estatisticaRepository.findByParticipacaoId(participacao.getId())).isEmpty();
    }

    /* ------------------------------------------------------------------ */

    private TotaisJogador totalDe(List<TotaisJogador> totais, Usuario usuario) {
        return totais.stream()
                .filter(linha -> linha.usuarioId().equals(usuario.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Nenhum total encontrado para o jogador " + usuario.getNickname()));
    }

    private Usuario persistirUsuario(String prefixo, Posicao posicao) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        return em.persist(Usuario.builder()
                .nomeCompleto(prefixo + " de teste")
                .nickname(prefixo + "-" + sufixo)
                .numeroCelular("(11) 91234-5678")
                .email(prefixo + "-" + sufixo + "@teste.com")
                .idade(30)
                .posicao(posicao)
                .nacionalidade("Brasileira")
                .estrelas(new BigDecimal("3.5"))
                .build());
    }

    private Pelada persistirPelada(Usuario organizador) {
        return em.persist(Pelada.builder()
                .nome("Pelada de teste")
                .data(LocalDate.now().plusDays(1))
                .horaInicio(LocalTime.of(19, 0))
                .horaFim(LocalTime.of(21, 0))
                .localNome("Quadra do teste")
                .endereco("Rua do teste, 100")
                .cidade("Sorocaba")
                .estado("SP")
                .tipoCampo(TipoCampo.SOCIETY)
                .maxParticipantes(20)
                .status(StatusPelada.FINALIZADA)
                .organizador(organizador)
                .build());
    }

    private void persistirEstatistica(Usuario jogador, Posicao posicaoJogada,
                                      int gols, int assistencias, int desarmes,
                                      int defesas, int defesasDificeis) {
        persistirEstatisticaEm(pelada, jogador, posicaoJogada,
                gols, assistencias, desarmes, defesas, defesasDificeis);
    }

    private void persistirEstatisticaEm(Pelada peladaAlvo, Usuario jogador, Posicao posicaoJogada,
                                        int gols, int assistencias, int desarmes,
                                        int defesas, int defesasDificeis) {
        ParticipacaoPelada participacao = em.persist(ParticipacaoPelada.builder()
                .pelada(peladaAlvo)
                .usuario(jogador)
                .status(StatusParticipacao.CONFIRMADO)
                .build());

        em.persist(EstatisticaPartida.builder()
                .participacao(participacao)
                .posicaoJogada(posicaoJogada)
                .gols(gols)
                .assistencias(assistencias)
                .desarmes(desarmes)
                .defesas(defesas)
                .defesasDificeis(defesasDificeis)
                .build());
    }
}
