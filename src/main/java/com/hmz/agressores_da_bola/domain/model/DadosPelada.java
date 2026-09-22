package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.model.enums.TipoCampo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Os dados editáveis de uma pelada, na linguagem do domínio. É o que a
 * entidade recebe para ser agendada ou atualizada — organizador e status
 * ficam de fora de propósito: têm regras e operações próprias.
 */
public record DadosPelada(
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
        Integer maxParticipantes,
        BigDecimal valorPorJogador
) {

    public LocalDateTime inicio() {
        return LocalDateTime.of(data, horaInicio);
    }
}
