package com.hmz.agressores_da_bola.domain.model.enums;

public enum TipoCampo implements Descritivel {

    FUTSAL("Futsal"),
    SOCIETY("Society");

    private final String descricao;

    TipoCampo(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
