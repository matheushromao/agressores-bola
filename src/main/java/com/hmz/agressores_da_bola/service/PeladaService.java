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
 *
 * <p>As operações de escrita recebem {@code usuarioLogadoId}, o usuário do
 * token: pelada só o organizador altera; na escalação o jogador também
 * cuida da própria participação.</p>
 */
public interface PeladaService {

    /* --------------------------- Pelada --------------------------- */

    /**
     * @param usuarioLogadoId vira o organizador da pelada
     */
    PeladaResponse criar(PeladaRequest request, Long usuarioLogadoId);

    PeladaResponse buscarPorId(Long id);

    PageResponse<PeladaResumoResponse> listar(PeladaFiltro filtro, Pageable pageable);

    PeladaResponse atualizar(Long id, PeladaRequest request, Long usuarioLogadoId);

    PeladaResponse alterarStatus(Long id, StatusPelada novoStatus, Long usuarioLogadoId);

    void deletar(Long id, Long usuarioLogadoId);

    /* ------------------------- Escalação -------------------------- */

    List<ParticipanteResponse> listarParticipantes(Long peladaId);

    ParticipanteResponse adicionarParticipante(Long peladaId, ParticipacaoRequest request, Long usuarioLogadoId);

    ParticipanteResponse alterarStatusParticipacao(Long peladaId, Long usuarioId, StatusParticipacao novoStatus,
                                                   Long usuarioLogadoId);

    void removerParticipante(Long peladaId, Long usuarioId, Long usuarioLogadoId);
}
