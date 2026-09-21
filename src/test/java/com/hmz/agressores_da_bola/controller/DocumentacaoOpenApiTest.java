package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O contrato OpenAPI é o que o frontend vai consumir, então ele próprio
 * precisa de teste. Em especial o acesso sem token: apertar o
 * {@code SecurityConfig} sem liberar as rotas do springdoc faz o Swagger
 * voltar a responder 401 em silêncio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DocumentacaoOpenApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("o contrato OpenAPI é servido sem token e cobre os endpoints da API")
    void contratoPublico() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Agressores da Bola"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/peladas']").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios']").exists())
                .andExpect(jsonPath("$.paths['/api/ranking']").exists())
                .andExpect(jsonPath("$.paths['/api/peladas/{peladaId}/sorteio']").exists())
                .andExpect(jsonPath("$.paths['/api/peladas/{peladaId}/estatisticas']").exists());
    }

    @Test
    @DisplayName("o Swagger UI abre sem token")
    void swaggerUiPublico() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("o esquema bearer-jwt está declarado e é exigido dos endpoints protegidos")
    void esquemaDeSegurancaDeclarado() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].bearerFormat").value("JWT"))
                // Sem "security" próprio, a operação herda a exigência global
                .andExpect(jsonPath("$.paths['/api/peladas'].post.security").doesNotExist())
                .andExpect(jsonPath("$.security[0]['bearer-jwt']").exists());
    }

    @Test
    @DisplayName("os endpoints públicos se declaram sem exigência de token")
    void endpointsPublicosSemExigencia() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/ranking'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/peladas'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty());
    }

    @Test
    @DisplayName("as respostas de erro estão documentadas com o schema ErroResponse")
    void respostasDeErroDocumentadas() throws Exception {
        String erroResponse = "#/components/schemas/ErroResponse";

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // 401 e 403 vêm do OpenApiCustomizer, sem anotação nos controllers
                .andExpect(jsonPath("$.paths['/api/peladas'].post.responses.401"
                        + ".content['application/json'].schema.$ref").value(erroResponse))
                .andExpect(jsonPath("$.paths['/api/peladas'].post.responses.403").exists())
                // e as metanotações de config/openapi precisam ser lidas como @ApiResponse
                .andExpect(jsonPath("$.paths['/api/peladas/{id}'].get.responses.404"
                        + ".content['application/json'].schema.$ref").value(erroResponse))
                .andExpect(jsonPath("$.components.schemas.ErroResponse.properties.campos").exists());
    }

    @Test
    @DisplayName("o Pageable vira parâmetros de query, não um objeto no corpo")
    void paginacaoComoParametroDeQuery() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/peladas'].get.parameters[?(@.name == 'page')]")
                        .value(not(emptyIterable())))
                .andExpect(jsonPath("$.paths['/api/peladas'].get.parameters[?(@.name == 'size')]")
                        .value(not(emptyIterable())))
                .andExpect(jsonPath("$.paths['/api/peladas'].get.parameters[?(@.name == 'cidade')]")
                        .value(not(emptyIterable())));
    }
}
