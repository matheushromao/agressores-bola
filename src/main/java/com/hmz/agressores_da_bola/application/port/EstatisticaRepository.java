package com.hmz.agressores_da_bola.application.port;

import com.hmz.agressores_da_bola.domain.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.domain.ranking.TotaisJogador;

import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência da súmula e das agregações dos rankings.
 */
public interface EstatisticaRepository {

    Optional<EstatisticaPartida> buscarDaParticipacao(Long participacaoId);

    /**
     * Súmula completa de uma pelada, com jogador e pelada carregados.
     */
    List<EstatisticaPartida> listarDaPelada(Long peladaId);

    /**
     * Soma as estatísticas por jogador — a base de todos os rankings.
     *
     * @param peladaId restringe a uma pelada; nulo vale o histórico completo
     */
    List<TotaisJogador> somarPorJogador(Long peladaId);

    EstatisticaPartida salvar(EstatisticaPartida estatistica);
}
