package com.hmz.agressores_da_bola.security;

import com.hmz.agressores_da_bola.exception.ErroResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Autenticação stateless por JWT (HS256) e a primeira camada de autorização:
 * quais rotas são públicas e quais exigem login. A segunda camada — quem é
 * dono de quê — vive nos services, porque é regra de negócio.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    /**
     * Leituras abertas: ranking, peladas, escalação e súmula. Os dados de
     * jogador ({@code /api/usuarios}) ficam de fora porque expõem e-mail e
     * celular.
     */
    private static final String[] LEITURAS_PUBLICAS = {
            "/api/ranking/**",
            "/api/peladas",
            "/api/peladas/*",
            "/api/peladas/*/participantes",
            "/api/peladas/*/estatisticas",
            "/api/peladas/*/participantes/*/estatistica"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (request, response, ex) ->
                escreverErro(jsonMapper, response, HttpStatus.UNAUTHORIZED, "Não autenticado",
                        "Faça login e envie o token no cabeçalho Authorization: Bearer <token>");
        AccessDeniedHandler acessoNegado = (request, response, ex) ->
                escreverErro(jsonMapper, response, HttpStatus.FORBIDDEN, "Acesso negado",
                        "Você não tem permissão para esta operação");

        http
                // Sem sessão nem cookie: o token vai em todo request, então CSRF não se aplica
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/auth/cadastro", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, LEITURAS_PUBLICAS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado));

        return http.build();
    }

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
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.EMISSOR));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    /**
     * BCrypt com prefixo de algoritmo ({@code {bcrypt}...}), o que permite
     * trocar de algoritmo no futuro sem invalidar as senhas já gravadas.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties propriedades) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(propriedades.origens());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * 401 e 403 barrados no filtro saem no mesmo formato do handler global,
     * para o cliente tratar um único contrato de erro.
     */
    private static void escreverErro(JsonMapper jsonMapper, HttpServletResponse response,
                                     HttpStatus status, String erro, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), ErroResponse.de(status.value(), erro, mensagem));
    }
}
