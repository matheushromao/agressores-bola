package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.pelada.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;

import java.util.List;

/**
 * Casos de uso da escalação (o "grupo" da pelada). O organizador mexe na
 * participação de qualquer um; o jogador cuida só da própria.
 */
public interface EscalacaoService {

    List<ParticipanteResponse> listar(Long peladaId);

    ParticipanteResponse adicionar(Long peladaId, Long usuarioId, StatusParticipacao status,
                                   Long usuarioLogadoId);

    ParticipanteResponse alterarStatus(Long peladaId, Long usuarioId, StatusParticipacao novoStatus,
                                       Long usuarioLogadoId);

    void remover(Long peladaId, Long usuarioId, Long usuarioLogadoId);
}
