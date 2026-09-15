package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.CadastroRequest;
import com.hmz.agressores_da_bola.dto.LoginRequest;
import com.hmz.agressores_da_bola.dto.TokenResponse;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.exception.CredenciaisInvalidasException;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.repository.UsuarioRepository;
import com.hmz.agressores_da_bola.security.TokenService;
import com.hmz.agressores_da_bola.service.AuthService;
import com.hmz.agressores_da_bola.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * Delega ao {@link UsuarioService} para reaproveitar as regras de nickname
     * e e-mail únicos, que valem igualmente no cadastro e na edição.
     */
    @Override
    public UsuarioResponse cadastrar(CadastroRequest request) {
        return usuarioService.criar(request.usuario(), request.senha());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .filter(encontrado -> passwordEncoder.matches(request.senha(), encontrado.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha incorretos"));

        return tokenService.emitir(usuario);
    }
}
