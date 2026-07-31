package com.hmz.agressores_da_bola.service;

import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.ParticipacaoRequest;
import com.hmz.agressores_da_bola.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.dto.PeladaFiltro;
import com.hmz.agressores_da_bola.dto.PeladaRequest;
import com.hmz.agressores_da_bola.dto.PeladaResponse;
import com.hmz.agressores_da_bola.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contrato do caso de uso de peladas. Separa dois grupos de operações:
 * o CRUD da pelada em si e a gestão da escalação (o "grupo" de jogadores).
 */
public interface PeladaService {

    /* --------------------------- Pelada --------------------------- */

    PeladaResponse criar(PeladaRequest request);

    PeladaResponse buscarPorId(Long id);

    PageResponse<PeladaResumoResponse> listar(PeladaFiltro filtro, Pageable pageable);

    PeladaResponse atualizar(Long id, PeladaRequest request);

    PeladaResponse alterarStatus(Long id, StatusPelada novoStatus);

    void deletar(Long id);

    /* ------------------------- Escalação -------------------------- */

    List<ParticipanteResponse> listarParticipantes(Long peladaId);

    ParticipanteResponse adicionarParticipante(Long peladaId, ParticipacaoRequest request);

    ParticipanteResponse alterarStatusParticipacao(Long peladaId, Long usuarioId, StatusParticipacao novoStatus);

    void removerParticipante(Long peladaId, Long usuarioId);
}
