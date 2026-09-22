package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.model.enums.Posicao;

import java.math.BigDecimal;

/**
 * Os dados de perfil de um jogador, iguais no cadastro e na edição. A senha
 * fica de fora: só existe no cadastro e tem tratamento próprio.
 */
public record PerfilUsuario(
        String nomeCompleto,
        String nickname,
        String descricao,
        String numeroCelular,
        String email,
        Integer idade,
        Posicao posicao,
        String nacionalidade,
        BigDecimal estrelas
) {
}
