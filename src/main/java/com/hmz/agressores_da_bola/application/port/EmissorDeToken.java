package com.hmz.agressores_da_bola.application.port;

import com.hmz.agressores_da_bola.application.usuario.dto.TokenResponse;
import com.hmz.agressores_da_bola.domain.model.Usuario;

/**
 * Emite o token de acesso de um usuário autenticado. O formato (JWT HS256
 * hoje) é detalhe da infraestrutura.
 */
public interface EmissorDeToken {

    TokenResponse emitir(Usuario usuario);
}
