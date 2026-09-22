package com.hmz.agressores_da_bola.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * O que a {@link Pelada} precisa saber da agenda do organizador para decidir
 * se um horário está livre. É uma interface do domínio (Dependency
 * Inversion): quem responde de verdade é a persistência, mas a regra
 * continua morando na entidade.
 */
public interface AgendaDePeladas {

    boolean organizadorTemPeladaEm(Long organizadorId, LocalDate data, LocalTime horaInicio);
}
