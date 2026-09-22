package com.hmz.agressores_da_bola.application.usuario;

import com.hmz.agressores_da_bola.domain.model.enums.Posicao;

/**
 * Filtros opcionais da listagem de jogadores; campo nulo significa "sem filtro".
 *
 * @param busca termo aplicado a nome completo ou nickname
 */
public record UsuarioFiltro(Posicao posicao, String busca, String nacionalidade) {
}
