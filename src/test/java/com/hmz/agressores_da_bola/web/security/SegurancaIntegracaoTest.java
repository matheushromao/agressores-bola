package com.hmz.agressores_da_bola.web.security;

import com.hmz.agressores_da_bola.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de ponta a ponta — cadastro, login, token e posse — contra o
 * MySQL do Testcontainers. Cada teste cria os próprios usuários, porque os
 * requests via MockMvc confirmam a transação e os dados ficam na base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SegurancaIntegracaoTest {

    private static final String SENHA = "senha-forte-123";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("ranking e lista de peladas são públicos")
    void leiturasPublicas() throws Exception {
        mvc.perform(get("/api/ranking")).andExpect(status().isOk());
        mvc.perform(get("/api/peladas")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("dados de jogador exigem login e o 401 sai no formato de erro da API")
    void dadosDeJogadorExigemLogin() throws Exception {
        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Não autenticado"));
    }

    @Test
    @DisplayName("o cadastro nunca devolve a senha, e senha errada no login dá 401")
    void cadastroELoginComSenhaErrada() throws Exception {
        String email = novoEmail();
        cadastrar(email)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senha").doesNotExist());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "senha-errada-999"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos"));
    }

    @Test
    @DisplayName("só o organizador altera a pelada; o jogador entra sozinho mas não inclui terceiros")
    void posseDaPelada() throws Exception {
        String emailA = novoEmail();
        String emailB = novoEmail();
        Integer idA = JsonPath.read(cadastrar(emailA).andReturn().getResponse().getContentAsString(), "$.id");
        Integer idB = JsonPath.read(cadastrar(emailB).andReturn().getResponse().getContentAsString(), "$.id");
        String tokenA = login(emailA);
        String tokenB = login(emailB);

        mvc.perform(post("/api/peladas").contentType(MediaType.APPLICATION_JSON).content(peladaJson()))
                .andExpect(status().isUnauthorized());

        String criada = mvc.perform(post("/api/peladas")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(peladaJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizador.id").value(idA))
                .andReturn().getResponse().getContentAsString();
        Integer peladaId = JsonPath.read(criada, "$.id");

        mvc.perform(put("/api/peladas/{id}", peladaId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(peladaJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(get("/api/peladas/{id}", peladaId)).andExpect(status().isOk());

        mvc.perform(post("/api/peladas/{id}/participantes", peladaId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\": %d}".formatted(idB)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/peladas/{id}/participantes", peladaId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\": %d}".formatted(idA)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token adulterado é recusado")
    void tokenAdulterado() throws Exception {
        String email = novoEmail();
        cadastrar(email);
        String token = login(email);
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /* ------------------------------------------------------------------ */

    private ResultActions cadastrar(String email) throws Exception {
        String nickname = "j" + UUID.randomUUID().toString().substring(0, 8);
        return mvc.perform(post("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "usuario": {
                            "nomeCompleto": "Jogador de Teste",
                            "nickname": "%s",
                            "numeroCelular": "(11) 91234-5678",
                            "email": "%s",
                            "idade": 30,
                            "posicao": "ALA",
                            "nacionalidade": "Brasileira"
                          },
                          "senha": "%s"
                        }
                        """.formatted(nickname, email, SENHA)));
    }

    private String login(String email) throws Exception {
        String resposta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "senha": "%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private static String novoEmail() {
        return "j" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
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
