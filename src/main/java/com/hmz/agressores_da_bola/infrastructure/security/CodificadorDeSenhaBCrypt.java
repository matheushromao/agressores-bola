package com.hmz.agressores_da_bola.infrastructure.security;

import com.hmz.agressores_da_bola.application.port.CodificadorDeSenha;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt com prefixo de algoritmo ({@code {bcrypt}...}), o que permite
 * trocar de algoritmo no futuro sem invalidar as senhas já gravadas.
 */
@Component
class CodificadorDeSenhaBCrypt implements CodificadorDeSenha {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public String codificar(String senha) {
        return passwordEncoder.encode(senha);
    }

    @Override
    public boolean confere(String senha, String senhaCodificada) {
        return passwordEncoder.matches(senha, senhaCodificada);
    }
}
