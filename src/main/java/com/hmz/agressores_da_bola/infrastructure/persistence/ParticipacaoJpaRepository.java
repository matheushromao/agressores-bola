package com.hmz.agressores_da_bola.infrastructure.persistence;

import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ParticipacaoJpaRepository extends JpaRepository<ParticipacaoPelada, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<ParticipacaoPelada> findByPeladaIdOrderByDataInscricaoAsc(Long peladaId);

    Optional<ParticipacaoPelada> findByPeladaIdAndUsuarioId(Long peladaId, Long usuarioId);
}
