package com.hmz.agressores_da_bola.web.security;

import com.hmz.agressores_da_bola.web.error.ErroResponse;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Autenticação stateless por JWT e a primeira camada de autorização: quais
 * rotas são públicas e quais exigem login. A segunda camada — quem é dono de
 * quê — vive nas entidades do domínio, porque é regra de negócio. A chave e o
 * decodificador do token vêm de {@code infrastructure.security.JwtConfig}.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

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

    /**
     * Contrato OpenAPI e Swagger UI. No perfil {@code prod} o springdoc fica
     * desligado por configuração, então estas rotas nem existem lá.
     */
    private static final String[] DOCUMENTACAO_PUBLICA = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
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
                        .requestMatchers(HttpMethod.GET, DOCUMENTACAO_PUBLICA).permitAll()
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
