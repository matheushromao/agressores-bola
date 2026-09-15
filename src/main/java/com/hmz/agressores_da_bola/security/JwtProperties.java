package com.hmz.agressores_da_bola.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuração do token de acesso ({@code app.jwt.*}).
 *
 * @param secret    segredo HMAC do HS256, vindo de {@code JWT_SECRET}; precisa
 *                  ter pelo menos 32 bytes
 * @param expiracao validade do token a partir da emissão
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration expiracao
) {
}
