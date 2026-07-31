package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Versão reduzida usada nas listagens paginadas: sem a escalação completa,
 * o payload de uma página com 20 peladas continua leve.
 */
public record PeladaResumoResponse(
        Long id,
        String nome,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String localNome,
        String cidade,
        String estado,
        TipoCampo tipoCampo,
        String tipoCampoDescricao,
        StatusPelada status,
        String statusDescricao,
        Integer maxParticipantes,
        Integer totalConfirmados,
        Integer vagasRestantes,
        BigDecimal valorPorJogador,
        String organizadorNickname
) {
}
