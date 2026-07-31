package com.hmz.agressores_da_bola.repository;

import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipacaoPeladaRepository extends JpaRepository<ParticipacaoPelada, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<ParticipacaoPelada> findByPeladaIdOrderByDataInscricaoAsc(Long peladaId);

    Optional<ParticipacaoPelada> findByPeladaIdAndUsuarioId(Long peladaId, Long usuarioId);

    boolean existsByPeladaIdAndUsuarioId(Long peladaId, Long usuarioId);

    long countByPeladaIdAndStatus(Long peladaId, StatusParticipacao status);
}
