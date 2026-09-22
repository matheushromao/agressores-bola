package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.domain.model.enums.TipoCampo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta uma pelada já em andamento de escalação, sem passar pelas regras de
 * agendamento — o jeito de pôr a entidade em qualquer estado para testar a
 * regra seguinte. O organizador entra sempre confirmado, como na vida real.
 */
public final class CenarioDePelada {

    public static final Long PELADA_ID = 10L;
    public static final Long ORGANIZADOR_ID = 1L;

    /** O "agora" de todos os testes, para nada depender do relógio da máquina. */
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 22, 12, 0);
    public static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final List<ParticipacaoPelada> participacoes = new ArrayList<>();
    private final Pelada pelada;

    private CenarioDePelada(int maxParticipantes, StatusPelada status) {
        Usuario organizador = usuario(ORGANIZADOR_ID);
        this.pelada = Pelada.builder()
                .id(PELADA_ID)
                .nome("Pelada de quinta")
                .data(AGORA.toLocalDate().plusDays(1))
                .horaInicio(LocalTime.of(19, 0))
                .horaFim(LocalTime.of(21, 0))
                .maxParticipantes(maxParticipantes)
                .status(status)
                .organizador(organizador)
                .participacoes(participacoes)
                .build();
        com(organizador, StatusParticipacao.CONFIRMADO);
    }

    public static CenarioDePelada pelada(int maxParticipantes, StatusPelada status) {
        return new CenarioDePelada(maxParticipantes, status);
    }

    public static CenarioDePelada peladaAgendada(int maxParticipantes) {
        return pelada(maxParticipantes, StatusPelada.AGENDADA);
    }

    /**
     * Inscreve o jogador depois de todos os anteriores, preservando a ordem
     * da lista de espera.
     */
    public CenarioDePelada com(Usuario usuario, StatusParticipacao status) {
        participacoes.add(ParticipacaoPelada.builder()
                .id(100L + usuario.getId())
                .pelada(pelada)
                .usuario(usuario)
                .status(status)
                .dataInscricao(AGORA.minusHours(10).plusMinutes(participacoes.size()))
                .build());
        return this;
    }

    public CenarioDePelada comConfirmados(int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            com(usuario(200L + i), StatusParticipacao.CONFIRMADO);
        }
        return this;
    }

    public Pelada pelada() {
        return pelada;
    }

    public ParticipacaoPelada participacaoDe(Long usuarioId) {
        return pelada.buscarParticipacaoDoUsuario(usuarioId).orElseThrow();
    }

    public static Usuario usuario(Long id) {
        return usuario(id, Posicao.ALA);
    }

    public static Usuario usuario(Long id, Posicao posicao) {
        return Usuario.builder()
                .id(id)
                .nickname("jogador" + id)
                .nomeCompleto("Jogador " + id)
                .posicao(posicao)
                .estrelas(new BigDecimal("3.0"))
                .build();
    }

    public static DadosPelada dadosValidos() {
        return dadosEm(AGORA.toLocalDate().plusDays(1), LocalTime.of(19, 0), 10);
    }

    public static DadosPelada dadosEm(LocalDate data, LocalTime horaInicio, int maxParticipantes) {
        return new DadosPelada("Pelada de quinta", null, data, horaInicio, horaInicio.plusHours(2),
                "Arena", "Rua A, 1", "Sorocaba", "sp", TipoCampo.SOCIETY, maxParticipantes, BigDecimal.TEN);
    }
}
