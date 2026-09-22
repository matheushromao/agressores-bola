package com.hmz.agressores_da_bola.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Chave, codificador e decodificador do token (HS256). O decodificador é o
 * que o resource server da camada web usa para validar cada requisição.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    @Bean
    SecretKey chaveJwt(JwtProperties propriedades) {
        byte[] bytes = propriedades.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa ter pelo menos " + TAMANHO_MINIMO_SEGREDO
                            + " bytes para HS256. Defina JWT_SECRET no ambiente ou no .env "
                            + "(por exemplo com: openssl rand -base64 48)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtTokenEmissor.EMISSOR));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }
}
