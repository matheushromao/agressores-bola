package com.hmz.agressores_da_bola.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(requiredProperties = {
        "nome",
        "quantidadeJogadores",
        "totalEstrelas",
        "mediaEstrelas",
        "temGoleiro",
        "jogadores"
})
public record TimeSorteadoResponse(
        String nome,
        int quantidadeJogadores,
        BigDecimal totalEstrelas,
        BigDecimal mediaEstrelas,
        boolean temGoleiro,
        List<JogadorSorteadoResponse> jogadores
) {
}
