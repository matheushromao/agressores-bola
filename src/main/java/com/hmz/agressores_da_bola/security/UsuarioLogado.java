package com.hmz.agressores_da_bola.security;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Extrai do token quem está chamando a API. Os services recebem só o id,
 * sem depender do Spring Security.
 */
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
