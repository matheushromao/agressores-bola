package com.hmz.agressores_da_bola.mapper;

import com.hmz.agressores_da_bola.dto.UsuarioRequest;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.dto.UsuarioResumoResponse;
import com.hmz.agressores_da_bola.model.Usuario;
import org.springframework.stereotype.Component;

/**
 * Responsável unicamente pela conversão entre entidade e DTOs,
 * mantendo o service livre de detalhes de transformação.
 */
@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioRequest request) {
        return Usuario.builder()
                .nomeCompleto(request.nomeCompleto())
                .nickname(request.nickname())
                .descricao(request.descricao())
                .numeroCelular(request.numeroCelular())
                .email(request.email())
                .idade(request.idade())
                .posicao(request.posicao())
                .nacionalidade(request.nacionalidade())
                .build();
    }

    public void copyToEntity(UsuarioRequest request, Usuario usuario) {
        usuario.setNomeCompleto(request.nomeCompleto());
        usuario.setNickname(request.nickname());
        usuario.setDescricao(request.descricao());
        usuario.setNumeroCelular(request.numeroCelular());
        usuario.setEmail(request.email());
        usuario.setIdade(request.idade());
        usuario.setPosicao(request.posicao());
        usuario.setNacionalidade(request.nacionalidade());
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNomeCompleto(),
                usuario.getNickname(),
                usuario.getDescricao(),
                usuario.getNumeroCelular(),
                usuario.getEmail(),
                usuario.getIdade(),
                usuario.getPosicao(),
                usuario.getPosicao() != null ? usuario.getPosicao().getDescricao() : null,
                usuario.getNacionalidade()
        );
    }

    /**
     * Visão reduzida usada quando o usuário aparece dentro de outro recurso
     * (organizador ou participante de uma pelada).
     */
    public UsuarioResumoResponse toResumoResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResumoResponse(
                usuario.getId(),
                usuario.getNickname(),
                usuario.getNomeCompleto(),
                usuario.getPosicao(),
                usuario.getPosicao() != null ? usuario.getPosicao().getDescricao() : null
        );
    }
}
