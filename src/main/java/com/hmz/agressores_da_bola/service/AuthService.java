package com.hmz.agressores_da_bola.service;

import com.hmz.agressores_da_bola.dto.CadastroRequest;
import com.hmz.agressores_da_bola.dto.LoginRequest;
import com.hmz.agressores_da_bola.dto.TokenResponse;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;

/**
 * Entrada na aplicação: criar a conta e trocar e-mail e senha por um token.
 */
public interface AuthService {

    UsuarioResponse cadastrar(CadastroRequest request);

    TokenResponse login(LoginRequest request);
}
