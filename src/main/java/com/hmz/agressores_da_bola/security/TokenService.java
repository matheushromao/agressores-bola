package com.hmz.agressores_da_bola.security;

import com.hmz.agressores_da_bola.dto.TokenResponse;
import com.hmz.agressores_da_bola.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Emite o token de acesso. O {@code sub} é o id do usuário — é o que os
 * controllers repassam aos services para checar a posse dos recursos.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    static final String EMISSOR = "agressores-da-bola";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propriedades;

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
