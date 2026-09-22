package com.hmz.agressores_da_bola.application.port;

import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;

import java.util.List;
import java.util.Optional;

/**
 * Porta de persistência da escalação.
 */
public interface ParticipacaoRepository {

    /**
     * Escalação da pelada por ordem de inscrição, já com os jogadores.
     */
    List<ParticipacaoPelada> listarDaPelada(Long peladaId);

    Optional<ParticipacaoPelada> buscar(Long peladaId, Long usuarioId);

    ParticipacaoPelada salvar(ParticipacaoPelada participacao);

    default ParticipacaoPelada obter(Long peladaId, Long usuarioId) {
        return buscar(peladaId, usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.participacao(peladaId, usuarioId));
    }
}
