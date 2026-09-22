package com.hmz.agressores_da_bola.application.port;

/**
 * Hash de senha. O caso de uso só sabe que a senha é codificada e conferida;
 * o algoritmo (BCrypt hoje) é detalhe da infraestrutura.
 */
public interface CodificadorDeSenha {

    String codificar(String senha);

    boolean confere(String senha, String senhaCodificada);
}
