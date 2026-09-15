package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.security.SecurityConfig;
import com.hmz.agressores_da_bola.service.PeladaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Camada web da pelada com a segurança real e o service simulado: confere
 * quais rotas são públicas, que o id do token chega ao service e que as
 * exceções de posse viram 403 — sem banco.
 */
@WebMvcTest(PeladaController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PeladaControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PeladaService peladaService;

    @Test
    @DisplayName("listar e detalhar peladas não exigem token")
    void leiturasPublicas() throws Exception {
        mvc.perform(get("/api/peladas")).andExpect(status().isOk());
        mvc.perform(get("/api/peladas/5")).andExpect(status().isOk());
        mvc.perform(get("/api/peladas/5/participantes")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("escrever sem token dá 401 sem chegar ao service")
    void escritaSemToken() throws Exception {
        mvc.perform(post("/api/peladas").contentType(MediaType.APPLICATION_JSON).content(peladaJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(delete("/api/peladas/5"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(peladaService);
    }

    @Test
    @DisplayName("o id do usuário vem do subject do token")
    void idDoTokenChegaAoService() throws Exception {
        mvc.perform(put("/api/peladas/5")
                        .with(jwt().jwt(token -> token.subject("42")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(peladaJson()))
                .andExpect(status().isOk());

        verify(peladaService).atualizar(eq(5L), any(), eq(42L));
    }

    @Test
    @DisplayName("acesso negado no service vira 403 no formato de erro da API")
    void acessoNegadoVira403() throws Exception {
        doThrow(new AcessoNegadoException("Só o organizador pode alterar esta pelada"))
                .when(peladaService).deletar(5L, 7L);

        mvc.perform(delete("/api/peladas/5").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Só o organizador pode alterar esta pelada"));
    }

    private static String peladaJson() {
        return """
                {
                  "nome": "Pelada de teste",
                  "data": "%s",
                  "horaInicio": "19:00",
                  "horaFim": "21:00",
                  "localNome": "Arena",
                  "endereco": "Rua A, 1",
                  "cidade": "Sorocaba",
                  "estado": "SP",
                  "tipoCampo": "SOCIETY",
                  "maxParticipantes": 10
                }
                """.formatted(LocalDate.now().plusDays(3));
    }
}
