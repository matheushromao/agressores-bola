package com.hmz.agressores_da_bola.application.usuario;

import com.hmz.agressores_da_bola.application.common.PageResponse;
import com.hmz.agressores_da_bola.application.port.CodificadorDeSenha;
import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.PerfilUsuario;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final CodificadorDeSenha codificadorDeSenha;

    @Override
    @Transactional
    public UsuarioResponse criar(PerfilUsuario perfil, String senha) {
        garantirNicknameDisponivel(perfil.nickname());
        garantirEmailDisponivel(perfil.email());

        Usuario usuario = Usuario.cadastrar(perfil, codificadorDeSenha.codificar(senha));
        return usuarioMapper.toResponse(usuarioRepository.salvar(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioMapper.toResponse(usuarioRepository.obter(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorNickname(String nickname) {
        Usuario usuario = usuarioRepository.buscarPorNickname(nickname)
                .orElseThrow(() -> RecursoNaoEncontradoException.usuarioComNickname(nickname));
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(UsuarioFiltro filtro, Pageable pageable) {
        return PageResponse.de(usuarioRepository.listar(filtro, pageable), usuarioMapper::toResponse);
    }

    @Override
    @Transactional
    public UsuarioResponse atualizar(Long id, PerfilUsuario perfil, Long usuarioLogadoId) {
        Usuario usuario = usuarioRepository.obter(id);
        usuario.exigirProprio(usuarioLogadoId);

        // Manter o próprio nickname ou e-mail não é conflito com ninguém.
        if (!usuario.temNickname(perfil.nickname())) {
            garantirNicknameDisponivel(perfil.nickname());
        }
        if (!usuario.temEmail(perfil.email())) {
            garantirEmailDisponivel(perfil.email());
        }

        usuario.atualizarPerfil(perfil);
        return usuarioMapper.toResponse(usuarioRepository.salvar(usuario));
    }

    @Override
    @Transactional
    public void deletar(Long id, Long usuarioLogadoId) {
        Usuario usuario = usuarioRepository.obter(id);
        usuario.exigirProprio(usuarioLogadoId);
        usuarioRepository.remover(usuario);
    }

    private void garantirNicknameDisponivel(String nickname) {
        if (usuarioRepository.nicknameEmUso(nickname)) {
            throw new RegraDeNegocioException("O nickname '" + nickname + "' já está em uso");
        }
    }

    private void garantirEmailDisponivel(String email) {
        if (usuarioRepository.emailEmUso(email)) {
            throw new RegraDeNegocioException("O e-mail '" + email + "' já está cadastrado");
        }
    }
}
