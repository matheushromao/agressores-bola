package com.hmz.agressores_da_bola.model.enums;

public enum StatusPelada {

    AGENDADA("Agendada"),
    CONFIRMADA("Confirmada"),
    EM_ANDAMENTO("Em andamento"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusPelada(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Uma pelada encerrada não aceita mais alterações de escalação.
     */
    public boolean encerrada() {
        return this == FINALIZADA || this == CANCELADA;
    }

    /**
     * Só faz sentido lançar súmula de uma pelada que já começou: antes disso
     * não houve jogo, e uma pelada cancelada nunca terá números.
     */
    public boolean aceitaEstatistica() {
        return this == EM_ANDAMENTO || this == FINALIZADA;
    }

    /**
     * O sorteio acontece antes da bola rolar, com a escalação já fechada.
     */
    public boolean aceitaSorteio() {
        return this != CANCELADA && this != FINALIZADA;
    }
}
