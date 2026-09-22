package com.hmz.agressores_da_bola.application.sorteio;

/**
 * Comando do sorteio: um dos dois critérios preenchido, o outro nulo.
 *
 * @param semente repetir a mesma semente reproduz exatamente o mesmo sorteio;
 *                nula, o sorteio sorteia uma
 */
public record CriterioSorteio(Integer quantidadeTimes, Integer jogadoresPorTime, Long semente) {
}
