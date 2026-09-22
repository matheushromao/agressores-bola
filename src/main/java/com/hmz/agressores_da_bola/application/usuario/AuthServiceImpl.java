package com.hmz.agressores_da_bola.application.usuario;

import com.hmz.agressores_da_bola.application.port.CodificadorDeSenha;
import com.hmz.agressores_da_bola.application.port.EmissorDeToken;
import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.application.usuario.dto.TokenResponse;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.domain.model.PerfilUsuario;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final CodificadorDeSenha codificadorDeSenha;
    private final EmissorDeToken emissorDeToken;

    /**
     * Delega ao {@link UsuarioService} para reaproveitar as regras de nickname
     * e e-mail únicos, que valem igualmente no cadastro e na edição.
     */
    @Override
    public UsuarioResponse cadastrar(PerfilUsuario perfil, String senha) {
        return usuarioService.criar(perfil, senha);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(String email, String senha) {
        Usuario usuario = usuarioRepository.buscarPorEmail(email)
                .filter(encontrado -> codificadorDeSenha.confere(senha, encontrado.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha incorretos"));

        return emissorDeToken.emitir(usuario);
    }
}
