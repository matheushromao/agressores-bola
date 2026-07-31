package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Resposta completa da pelada, com a escalação. Usada nos endpoints de
 * detalhe/criação/atualização.
 */
public record PeladaResponse(
        Long id,
        String nome,
        String descricao,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFim,
        String localNome,
        String endereco,
        String cidade,
        String estado,
        TipoCampo tipoCampo,
        String tipoCampoDescricao,
        Integer maxParticipantes,
        Integer totalConfirmados,
        Integer vagasRestantes,
        BigDecimal valorPorJogador,
        StatusPelada status,
        String statusDescricao,
        UsuarioResumoResponse organizador,
        List<ParticipanteResponse> participantes
) {
}
