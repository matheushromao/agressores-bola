package com.hmz.agressores_da_bola.infrastructure.persistence;

import com.hmz.agressores_da_bola.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

interface UsuarioJpaRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByNickname(String nickname);

    Optional<Usuario> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);
}
