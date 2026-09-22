package com.hmz.agressores_da_bola.application.port;

import com.hmz.agressores_da_bola.application.usuario.UsuarioFiltro;
import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Porta de persistência dos jogadores.
 */
public interface UsuarioRepository {

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorNickname(String nickname);

    Optional<Usuario> buscarPorEmail(String email);

    boolean nicknameEmUso(String nickname);

    boolean emailEmUso(String email);

    Page<Usuario> listar(UsuarioFiltro filtro, Pageable pageable);

    Usuario salvar(Usuario usuario);

    void remover(Usuario usuario);

    default Usuario obter(Long id) {
        return buscarPorId(id).orElseThrow(() -> RecursoNaoEncontradoException.usuario(id));
    }
}
