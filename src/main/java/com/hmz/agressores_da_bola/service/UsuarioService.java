package com.hmz.agressores_da_bola.service;

import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.UsuarioRequest;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import org.springframework.data.domain.Pageable;

/**
 * Contrato do caso de uso de usuários. O controller depende desta abstração
 * (Dependency Inversion), nunca da implementação concreta.
 */
public interface UsuarioService {

    /**
     * Cria o usuário com a senha já transformada em hash. Chamado pelo
     * cadastro em {@code /api/auth/cadastro}.
     */
    UsuarioResponse criar(UsuarioRequest request, String senha);

    UsuarioResponse buscarPorId(Long id);

    UsuarioResponse buscarPorNickname(String nickname);

    /**
     * Listagem paginada com filtros opcionais. A paginação é responsabilidade
     * do backend: o banco devolve apenas a fatia pedida.
     *
     * @param posicao       filtra pela posição em campo (opcional)
     * @param busca         termo aplicado a nome completo ou nickname (opcional)
     * @param nacionalidade filtra pela nacionalidade exata (opcional)
     */
    PageResponse<UsuarioResponse> listar(Posicao posicao, String busca, String nacionalidade, Pageable pageable);

    /**
     * @param usuarioLogadoId quem está chamando; só pode alterar o próprio cadastro
     */
    UsuarioResponse atualizar(Long id, UsuarioRequest request, Long usuarioLogadoId);

    /**
     * @param usuarioLogadoId quem está chamando; só pode apagar o próprio cadastro
     */
    void deletar(Long id, Long usuarioLogadoId);
}
