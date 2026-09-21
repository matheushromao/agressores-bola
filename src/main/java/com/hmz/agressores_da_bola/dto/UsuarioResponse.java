package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Perfil completo do jogador. `descricao` e `estrelas` são opcionais no cadastro e podem vir nulos.",
        requiredProperties = {
        "id",
        "nomeCompleto",
        "nickname",
        "numeroCelular",
        "email",
        "idade",
        "posicao",
        "posicaoDescricao",
        "nacionalidade"
})
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
