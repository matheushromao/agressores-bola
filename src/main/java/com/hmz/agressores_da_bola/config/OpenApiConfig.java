package com.hmz.agressores_da_bola.config;

import com.hmz.agressores_da_bola.exception.ErroResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Contrato OpenAPI da API: identificação, esquema de autenticação e as
 * respostas de erro que valem para todo endpoint protegido.
 *
 * <p>A configuração é programática de propósito — o {@code @OpenAPIDefinition}
 * equivalente ficaria pendurado na classe de aplicação, misturando a
 * documentação com o bootstrap.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Nome do esquema de segurança. É o que o botão <em>Authorize</em> do
     * Swagger UI usa para mandar {@code Authorization: Bearer <token>}, o
     * mesmo cabeçalho que o resource server espera.
     */
    public static final String ESQUEMA_JWT = "bearer-jwt";

    private static final String SCHEMA_ERRO = "#/components/schemas/ErroResponse";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agressores da Bola")
                        .version("v1")
                        .description("""
                                API REST para organização de peladas: cadastro de jogadores, \
                                agendamento de partidas, escalação com lista de espera, sorteio \
                                de times equilibrado por estrelas, súmula e rankings.

                                Autentique-se em `POST /api/auth/login` e use o token no botão \
                                **Authorize**. Endpoints sem cadeado são públicos.""")
                        .contact(new Contact()
                                .name("Matheus Romão")
                                .url("https://github.com/matheushromao"))
                        .license(new License()
                                .name("Projeto pessoal — todos os direitos reservados ao autor")))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Ambiente local")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido por POST /api/auth/login")))
                // Exigência padrão: vale para todo endpoint que não se declarar
                // público com @SecurityRequirements({})
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }

    /**
     * 401 e 403 valem para toda operação protegida. Documentá-las aqui evita
     * repetir dois {@code @ApiResponse} em cada um dos ~20 métodos de
     * controller — e garante que nenhum fique de fora por esquecimento.
     */
    @Bean
    OpenApiCustomizer respostasDeErroPadrao() {
        return openApi -> {
            registrarSchemaDeErro(openApi);

            openApi.getPaths().values().stream()
                    .flatMap(caminho -> caminho.readOperations().stream())
                    .filter(OpenApiConfig::exigeAutenticacao)
                    .forEach(operacao -> {
                        operacao.getResponses().addApiResponse("401",
                                respostaDeErro("Token ausente, inválido ou expirado"));
                        operacao.getResponses().addApiResponse("403",
                                respostaDeErro("Autenticado, mas sem permissão sobre este recurso"));
                    });
        };
    }

    /**
     * Sem {@code security} declarado, a operação herda a exigência global. Uma
     * lista vazia é o que {@code @SecurityRequirements({})} produz e marca o
     * endpoint como público.
     */
    private static boolean exigeAutenticacao(Operation operacao) {
        return operacao.getSecurity() == null || !operacao.getSecurity().isEmpty();
    }

    private static void registrarSchemaDeErro(OpenAPI openApi) {
        if (openApi.getComponents().getSchemas() != null
                && openApi.getComponents().getSchemas().containsKey("ErroResponse")) {
            return;
        }
        ModelConverters.getInstance()
                .readAll(new AnnotatedType(ErroResponse.class))
                .forEach(openApi.getComponents()::addSchemas);
    }

    private static ApiResponse respostaDeErro(String descricao) {
        return new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(SCHEMA_ERRO))));
    }
}
