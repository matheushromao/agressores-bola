package com.hmz.agressores_da_bola.domain.model.enums;

/**
 * Domínio fechado que tem um nome legível para exibir na tela. Existe para
 * que a conversão "enum → descrição" seja escrita uma vez só, em vez de um
 * {@code x != null ? x.getDescricao() : null} em cada mapper.
 */
public interface Descritivel {

    String getDescricao();

    static String descricaoDe(Descritivel valor) {
        return valor == null ? null : valor.getDescricao();
    }
}
