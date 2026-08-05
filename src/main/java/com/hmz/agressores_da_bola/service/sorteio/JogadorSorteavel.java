package com.hmz.agressores_da_bola.service.sorteio;

import com.hmz.agressores_da_bola.model.Usuario;

import java.math.BigDecimal;

/**
 * O jogador visto pelo sorteio: só o que interessa para equilibrar os times.
 * O {@link Usuario} vem junto apenas para montar a resposta no fim.
 */
public record JogadorSorteavel(Usuario usuario, BigDecimal estrelas, boolean goleiro) {

    public static JogadorSorteavel de(Usuario usuario) {
        return new JogadorSorteavel(usuario, usuario.estrelasOuPadrao(), usuario.ehGoleiro());
    }
}
