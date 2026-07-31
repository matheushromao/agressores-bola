package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;

/**
 * Visão enxuta do usuário, usada quando ele aparece dentro de outro recurso
 * (organizador ou participante de uma pelada), sem vazar e-mail e telefone.
 */
public record UsuarioResumoResponse(
        Long id,
        String nickname,
        String nomeCompleto,
        Posicao posicao,
        String posicaoDescricao
) {
}
