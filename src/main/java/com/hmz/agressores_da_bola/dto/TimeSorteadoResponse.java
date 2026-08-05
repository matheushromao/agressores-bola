package com.hmz.agressores_da_bola.dto;

import java.math.BigDecimal;
import java.util.List;

public record TimeSorteadoResponse(
        String nome,
        int quantidadeJogadores,
        BigDecimal totalEstrelas,
        BigDecimal mediaEstrelas,
        boolean temGoleiro,
        List<JogadorSorteadoResponse> jogadores
) {
}
