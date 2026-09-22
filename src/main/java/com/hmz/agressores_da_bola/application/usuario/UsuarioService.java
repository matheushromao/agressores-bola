package com.hmz.agressores_da_bola.application.usuario;

import com.hmz.agressores_da_bola.application.common.PageResponse;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.domain.model.PerfilUsuario;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de perfil de jogador. O controller depende desta abstração
 * (Dependency Inversion), nunca da implementação concreta.
 */
public interface UsuarioService {

    /**
     * Cria o usuário com a senha já transformada em hash. Chamado pelo
     * cadastro em {@code /api/auth/cadastro}.
     */
    UsuarioResponse criar(PerfilUsuario perfil, String senha);

    UsuarioResponse buscarPorId(Long id);

    UsuarioResponse buscarPorNickname(String nickname);

    /**
     * Listagem paginada com filtros opcionais. A paginação é responsabilidade
     * do backend: o banco devolve apenas a fatia pedida.
     */
    PageResponse<UsuarioResponse> listar(UsuarioFiltro filtro, Pageable pageable);

    /**
     * @param usuarioLogadoId quem está chamando; só pode alterar o próprio cadastro
     */
    UsuarioResponse atualizar(Long id, PerfilUsuario perfil, Long usuarioLogadoId);

    /**
     * @param usuarioLogadoId quem está chamando; só pode apagar o próprio cadastro
     */
    void deletar(Long id, Long usuarioLogadoId);
}
