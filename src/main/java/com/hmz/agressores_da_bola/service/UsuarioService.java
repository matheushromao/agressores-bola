package com.hmz.agressores_da_bola.service;

import com.hmz.agressores_da_bola.dto.UsuarioRequest;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.model.enums.Posicao;

import java.util.List;

/**
 * Contrato do caso de uso de usuários. O controller depende desta abstração
 * (Dependency Inversion), nunca da implementação concreta.
 */
public interface UsuarioService {

    UsuarioResponse criar(UsuarioRequest request);

    UsuarioResponse buscarPorId(Long id);

    UsuarioResponse buscarPorNickname(String nickname);

    List<UsuarioResponse> listarTodos();

    List<UsuarioResponse> listarPorPosicao(Posicao posicao);

    UsuarioResponse atualizar(Long id, UsuarioRequest request);

    void deletar(Long id);
}
