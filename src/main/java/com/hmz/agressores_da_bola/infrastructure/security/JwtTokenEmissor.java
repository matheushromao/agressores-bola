package com.hmz.agressores_da_bola.infrastructure.security;

import com.hmz.agressores_da_bola.application.port.EmissorDeToken;
import com.hmz.agressores_da_bola.application.usuario.dto.TokenResponse;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Emite o token de acesso em JWT HS256. O {@code sub} é o id do usuário —
 * é o que os controllers repassam aos casos de uso para checar a posse dos
 * recursos.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenEmissor implements EmissorDeToken {

    public static final String EMISSOR = "agressores-da-bola";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propriedades;

    @Override
    public TokenResponse emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(propriedades.expiracao());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getId().toString())
                .claim("nickname", usuario.getNickname())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(token, "Bearer", expiraEm);
    }
}
