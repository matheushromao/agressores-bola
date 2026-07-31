package com.hmz.agressores_da_bola.model.enums;

public enum TipoCampo {

    CAMPO("Campo de futebol"),
    SOCIETY("Society"),
    FUTSAL("Futsal"),
    QUADRA("Quadra poliesportiva"),
    AREIA("Futebol de areia");

    private final String descricao;

    TipoCampo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
