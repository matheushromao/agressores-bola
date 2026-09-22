package com.hmz.agressores_da_bola.application.estatistica;

import com.hmz.agressores_da_bola.application.estatistica.dto.EstatisticaResponse;

import java.util.List;

/**
 * Contrato do lançamento de súmula. É uma operação por jogador e por pelada:
 * registrar de novo sobrescreve os números, o que permite ao organizador
 * corrigir um lançamento sem precisar apagar antes.
 *
 * <p>Lançar e apagar súmula é exclusivo do organizador da pelada.</p>
 */
public interface EstatisticaService {

    EstatisticaResponse registrar(Long peladaId, Long usuarioId, LancamentoEstatistica lancamento,
                                  Long usuarioLogadoId);

    EstatisticaResponse buscar(Long peladaId, Long usuarioId);

    /**
     * Súmula da pelada inteira, do jogador que mais pontuou ao que menos
     * pontuou.
     */
    List<EstatisticaResponse> listarDaPelada(Long peladaId);

    void remover(Long peladaId, Long usuarioId, Long usuarioLogadoId);
}
