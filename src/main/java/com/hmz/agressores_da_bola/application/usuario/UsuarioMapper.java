package com.hmz.agressores_da_bola.application.usuario;

import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResumoResponse;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import org.springframework.stereotype.Component;

import static com.hmz.agressores_da_bola.domain.model.enums.Descritivel.descricaoDe;

/**
 * Monta as respostas do usuário. Criar e editar o perfil é comportamento da
 * própria entidade {@link Usuario}.
 */
@Component
public class UsuarioMapper {

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
                descricaoDe(usuario.getPosicao()),
                usuario.getNacionalidade(),
                usuario.getEstrelas()
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
                descricaoDe(usuario.getPosicao()),
                usuario.estrelasOuPadrao()
        );
    }
}
