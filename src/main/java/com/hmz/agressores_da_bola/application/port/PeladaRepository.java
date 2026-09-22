package com.hmz.agressores_da_bola.application.port;

import com.hmz.agressores_da_bola.application.pelada.PeladaFiltro;
import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.model.AgendaDePeladas;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Porta de persistência das peladas. Os casos de uso dependem desta
 * interface; quem a implementa é o adapter JPA da infraestrutura.
 */
public interface PeladaRepository extends AgendaDePeladas {

    Optional<Pelada> buscarPorId(Long id);

    /**
     * Carrega a pelada já com organizador e escalação.
     */
    Optional<Pelada> buscarComParticipantes(Long id);

    boolean existe(Long id);

    Page<Pelada> listar(PeladaFiltro filtro, Pageable pageable);

    Pelada salvar(Pelada pelada);

    void remover(Pelada pelada);

    default Pelada obter(Long id) {
        return buscarPorId(id).orElseThrow(() -> RecursoNaoEncontradoException.pelada(id));
    }

    default Pelada obterComParticipantes(Long id) {
        return buscarComParticipantes(id).orElseThrow(() -> RecursoNaoEncontradoException.pelada(id));
    }

    default void garantirQueExiste(Long id) {
        if (!existe(id)) {
            throw RecursoNaoEncontradoException.pelada(id);
        }
    }
}
