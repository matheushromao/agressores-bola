package com.hmz.agressores_da_bola.dto;

import com.hmz.agressores_da_bola.model.enums.Posicao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Visão enxuta do usuário, usada quando ele aparece dentro de outro recurso
 * (organizador ou participante de uma pelada), sem vazar e-mail e telefone.
 * Traz as estrelas porque é essa a visão que aparece no sorteio e no ranking.
 */
@Schema(requiredProperties = {
        "id",
        "nickname",
        "nomeCompleto",
        "posicao",
        "posicaoDescricao",
        "estrelas"
})
public record UsuarioResumoResponse(
        Long id,
        String nickname,
        String nomeCompleto,
        Posicao posicao,
        String posicaoDescricao,
        BigDecimal estrelas
) {
}
