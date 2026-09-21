package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.config.openapi.RespostaDadosInvalidos;
import com.hmz.agressores_da_bola.config.openapi.RespostaRegraDeNegocio;
import com.hmz.agressores_da_bola.dto.CadastroRequest;
import com.hmz.agressores_da_bola.dto.LoginRequest;
import com.hmz.agressores_da_bola.dto.TokenResponse;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.exception.ErroResponse;
import com.hmz.agressores_da_bola.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Rotas públicas de entrada: cadastro e login.
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro de jogador e emissão do token JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(operationId = "cadastrar", summary = "Cadastra um jogador",
            description = "Cria o perfil com a senha já codificada em BCrypt. "
                    + "A senha nunca volta em nenhuma resposta da API.")
    @ApiResponse(responseCode = "201", description = "Jogador cadastrado; o Location aponta para o perfil")
    @RespostaDadosInvalidos
    @RespostaRegraDeNegocio
    @SecurityRequirements
    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid CadastroRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        UsuarioResponse response = authService.cadastrar(request);
        URI location = uriBuilder.path("/api/usuarios/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(operationId = "login", summary = "Autentica e devolve o token",
            description = "Use o token no botão **Authorize** ou no cabeçalho "
                    + "`Authorization: Bearer <token>`.")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErroResponse.class)))
    @RespostaDadosInvalidos
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
