package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;

import java.math.BigDecimal;

public record UsuarioResponse(
        Long id,
        String nomeCompleto,
        String nickname,
        String descricao,
        String numeroCelular,
        String email,
        Integer idade,
        Posicao posicao,
        String posicaoDescricao,
        String nacionalidade,
        BigDecimal estrelas
) {
}
