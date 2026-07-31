package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;

import java.time.LocalDate;

/**
 * Agrupa os filtros opcionais da listagem de peladas. Existe para o service
 * não crescer uma assinatura com sete parâmetros soltos — qualquer campo
 * nulo significa "sem filtro".
 */
public record PeladaFiltro(
        StatusPelada status,
        TipoCampo tipoCampo,
        String cidade,
        LocalDate dataInicial,
        LocalDate dataFinal,
        Long organizadorId,
        Long participanteId
) {

    public static PeladaFiltro vazio() {
        return new PeladaFiltro(null, null, null, null, null, null, null);
    }
}
