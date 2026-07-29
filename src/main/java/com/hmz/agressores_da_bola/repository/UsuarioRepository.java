package com.hmz.agressores_da_bola.repository;

import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNickname(String nickname);

    Optional<Usuario> findByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    List<Usuario> findByPosicao(Posicao posicao);

    List<Usuario> findByNomeCompletoContainingIgnoreCase(String nomeCompleto);
}
