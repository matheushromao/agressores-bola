package com.hmz.agressores_da_bola.repository;

import com.hmz.agressores_da_bola.model.Pelada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface PeladaRepository extends JpaRepository<Pelada, Long>, JpaSpecificationExecutor<Pelada> {

    /**
     * Carrega a pelada já com organizador e escalação para a tela de detalhe,
     * evitando consultas extras na hora de montar o response.
     */
    @EntityGraph(attributePaths = {"organizador", "participacoes", "participacoes.usuario"})
    @Query("select p from Pelada p where p.id = :id")
    Optional<Pelada> buscarComParticipantes(@Param("id") Long id);

    /**
     * Sobrescrito apenas para pendurar o @EntityGraph: o organizador aparece
     * no resumo de toda listagem, então traze-lo junto evita N+1.
     * Como é uma associação ToOne, o join não interfere na paginação.
     */
    @Override
    @EntityGraph(attributePaths = "organizador")
    Page<Pelada> findAll(Specification<Pelada> spec, Pageable pageable);

    boolean existsByOrganizadorIdAndDataAndHoraInicio(Long organizadorId, LocalDate data, LocalTime horaInicio);
}
