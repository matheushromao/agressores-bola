package com.hmz.agressores_da_bola.application.usuario;

import com.hmz.agressores_da_bola.application.usuario.dto.TokenResponse;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.domain.model.PerfilUsuario;

/**
 * Entrada na aplicação: criar a conta e trocar e-mail e senha por um token.
 */
public interface AuthService {

    UsuarioResponse cadastrar(PerfilUsuario perfil, String senha);

    TokenResponse login(String email, String senha);
}
