package com.hmz.agressores_da_bola.infrastructure.persistence;

import com.hmz.agressores_da_bola.application.pelada.PeladaFiltro;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.infrastructure.persistence.specification.PeladaSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class PeladaRepositoryAdapter implements PeladaRepository {

    private final PeladaJpaRepository jpa;

    @Override
    public Optional<Pelada> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Pelada> buscarComParticipantes(Long id) {
        return jpa.buscarComParticipantes(id);
    }

    @Override
    public boolean existe(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public Page<Pelada> listar(PeladaFiltro filtro, Pageable pageable) {
        return jpa.findAll(PeladaSpecification.de(filtro), pageable);
    }

    @Override
    public boolean organizadorTemPeladaEm(Long organizadorId, LocalDate data, LocalTime horaInicio) {
        return jpa.existsByOrganizadorIdAndDataAndHoraInicio(organizadorId, data, horaInicio);
    }

    @Override
    public Pelada salvar(Pelada pelada) {
        return jpa.save(pelada);
    }

    @Override
    public void remover(Pelada pelada) {
        jpa.delete(pelada);
    }
}
