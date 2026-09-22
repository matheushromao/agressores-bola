package com.hmz.agressores_da_bola.domain.model.enums;

public enum Posicao implements Descritivel {

    GOLEIRO("Goleiro"),
    ALA("Ala"),
    FIXO("Fixo"),
    PIVO("Pivô"),
    AMADOR("Amador");

    private final String descricao;

    Posicao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }
}
