package com.hmz.agressores_da_bola.model.enums;

public enum Posicao {

    GOLEIRO("Goleiro"),
    ZAGUEIRO("Zagueiro"),
    LATERAL_DIREITO("Lateral direito"),
    LATERAL_ESQUERDO("Lateral esquerdo"),
    VOLANTE("Volante"),
    MEIA("Meia"),
    PONTA_DIREITA("Ponta direita"),
    PONTA_ESQUERDA("Ponta esquerda"),
    ATACANTE("Atacante");

    private final String descricao;

    Posicao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
