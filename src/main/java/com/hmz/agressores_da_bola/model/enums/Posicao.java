package com.hmz.agressores_da_bola.model.enums;

public enum Posicao {

    GOLEIRO("Goleiro"),
    ALA("Ala"),
    FIXO("Fixo"),
    PIVO("Pivô"),
    AMADOR("Amador");

    private final String descricao;

    Posicao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
