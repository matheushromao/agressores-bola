package com.hmz.agressores_da_bola.model.enums;

public enum TipoCampo {

    FUTSAL("Futsal"),
    SOCIETY("Society");

    private final String descricao;

    TipoCampo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
