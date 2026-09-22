package com.hmz.agressores_da_bola.infrastructure.persistence;

import com.hmz.agressores_da_bola.application.port.ParticipacaoRepository;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class ParticipacaoRepositoryAdapter implements ParticipacaoRepository {

    private final ParticipacaoJpaRepository jpa;

    @Override
    public List<ParticipacaoPelada> listarDaPelada(Long peladaId) {
        return jpa.findByPeladaIdOrderByDataInscricaoAsc(peladaId);
    }

    @Override
    public Optional<ParticipacaoPelada> buscar(Long peladaId, Long usuarioId) {
        return jpa.findByPeladaIdAndUsuarioId(peladaId, usuarioId);
    }

    @Override
    public ParticipacaoPelada salvar(ParticipacaoPelada participacao) {
        return jpa.save(participacao);
    }
}
