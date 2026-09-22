package com.hmz.agressores_da_bola.web.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Origens autorizadas a chamar a API pelo navegador ({@code app.cors.origens}),
 * tipicamente o endereço do frontend.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> origens) {

    public CorsProperties {
        origens = origens == null ? List.of() : List.copyOf(origens);
    }
}
