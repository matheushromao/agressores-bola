package com.hmz.agressores_da_bola.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado do sorteio.
 *
 * @param diferencaEntreTimes distância em estrelas entre o time mais forte e o
 *                            mais fraco: quanto mais perto de zero, mais
 *                            equilibrada ficou a divisão
 * @param reservas            confirmados que sobraram da divisão exata, para
 *                            que todos os times tenham o mesmo tamanho
 * @param semente             semente usada; devolver o mesmo valor no próximo
 *                            pedido repete este sorteio
 */
public record SorteioResponse(
        Long peladaId,
        String peladaNome,
        int quantidadeTimes,
        int jogadoresPorTime,
        int totalConfirmados,
        BigDecimal diferencaEntreTimes,
        List<TimeSorteadoResponse> times,
        List<JogadorSorteadoResponse> reservas,
        long semente
) {
}
