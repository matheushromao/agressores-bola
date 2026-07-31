package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.UsuarioRequest;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.UsuarioMapper;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.repository.UsuarioRepository;
import com.hmz.agressores_da_bola.repository.specification.UsuarioSpecification;
import com.hmz.agressores_da_bola.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        validarNicknameDisponivel(request.nickname(), null);
        validarEmailDisponivel(request.email(), null);

        Usuario usuario = usuarioMapper.toEntity(request);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return usuarioMapper.toResponse(obterUsuario(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorNickname(String nickname) {
        Usuario usuario = usuarioRepository.findByNickname(nickname)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com o nickname: " + nickname));
        return usuarioMapper.toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Posicao posicao, String busca, String nacionalidade, Pageable pageable) {
        Specification<Usuario> filtros = Specification.allOf(
                UsuarioSpecification.comPosicao(posicao),
                UsuarioSpecification.comNomeOuNickname(busca),
                UsuarioSpecification.daNacionalidade(nacionalidade)
        );

        Page<Usuario> pagina = usuarioRepository.findAll(filtros, pageable);
        return PageResponse.de(pagina, usuarioMapper::toResponse);
    }

    @Override
    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = obterUsuario(id);

        validarNicknameDisponivel(request.nickname(), usuario);
        validarEmailDisponivel(request.email(), usuario);

        usuarioMapper.copyToEntity(request, usuario);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        usuarioRepository.delete(obterUsuario(id));
    }

    private Usuario obterUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com o id: " + id));
    }

    private void validarNicknameDisponivel(String nickname, Usuario usuarioAtual) {
        if (usuarioAtual != null && nickname.equalsIgnoreCase(usuarioAtual.getNickname())) {
            return;
        }
        if (usuarioRepository.existsByNickname(nickname)) {
            throw new RegraDeNegocioException("O nickname '" + nickname + "' já está em uso");
        }
    }

    private void validarEmailDisponivel(String email, Usuario usuarioAtual) {
        if (usuarioAtual != null && email.equalsIgnoreCase(usuarioAtual.getEmail())) {
            return;
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("O e-mail '" + email + "' já está cadastrado");
        }
    }
}
