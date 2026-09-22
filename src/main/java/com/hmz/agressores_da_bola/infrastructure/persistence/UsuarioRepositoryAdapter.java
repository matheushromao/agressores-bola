package com.hmz.agressores_da_bola.infrastructure.persistence;

import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.application.usuario.UsuarioFiltro;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import com.hmz.agressores_da_bola.infrastructure.persistence.specification.UsuarioSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Usuario> buscarPorNickname(String nickname) {
        return jpa.findByNickname(nickname);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public boolean nicknameEmUso(String nickname) {
        return jpa.existsByNickname(nickname);
    }

    @Override
    public boolean emailEmUso(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public Page<Usuario> listar(UsuarioFiltro filtro, Pageable pageable) {
        return jpa.findAll(UsuarioSpecification.de(filtro), pageable);
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        return jpa.save(usuario);
    }

    @Override
    public void remover(Usuario usuario) {
        jpa.delete(usuario);
    }
}
