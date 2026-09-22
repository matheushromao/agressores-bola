package com.hmz.agressores_da_bola.application.estatistica;

import com.hmz.agressores_da_bola.domain.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;

/**
 * Comando de lançamento da súmula de um jogador.
 *
 * @param posicaoJogada posição efetivamente jogada; nula, vale a do cadastro
 */
public record LancamentoEstatistica(ResumoEstatistico numeros, Posicao posicaoJogada) {
}
