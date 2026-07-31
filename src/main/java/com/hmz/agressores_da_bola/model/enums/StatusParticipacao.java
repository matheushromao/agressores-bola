package com.hmz.agressores_da_bola.model.enums;

public enum StatusParticipacao {

    CONVIDADO("Convidado"),
    CONFIRMADO("Confirmado"),
    RECUSADO("Recusado"),
    LISTA_DE_ESPERA("Lista de espera");

    private final String descricao;

    StatusParticipacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Só quem está confirmado ocupa uma vaga da pelada.
     */
    public boolean ocupaVaga() {
        return this == CONFIRMADO;
    }
}
