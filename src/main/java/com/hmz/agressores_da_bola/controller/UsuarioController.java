package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.config.openapi.RespostaDadosInvalidos;
import com.hmz.agressores_da_bola.config.openapi.RespostaNaoEncontrado;
import com.hmz.agressores_da_bola.config.openapi.RespostaRegraDeNegocio;
import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.UsuarioRequest;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.security.UsuarioLogado;
import com.hmz.agressores_da_bola.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Perfis de jogador. O cadastro fica em {@code POST /api/auth/cadastro},
 * junto com a senha.
 */
@RestController
@RequestMapping(value = "/api/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Usuários",
        description = "Perfis de jogador. Exigem token: os dados expõem e-mail e celular. "
                + "O cadastro fica em POST /api/auth/cadastro.")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Listagem paginada: {@code ?page=0&size=10&sort=nomeCompleto,asc}.
     * O @PageableDefault define o comportamento quando o cliente não manda
     * nada, então o backend nunca devolve a tabela inteira.
     */
    @Operation(operationId = "listarUsuarios", summary = "Lista jogadores com filtros e paginação",
            description = "Sem `size` a página vem com 10 itens; o teto é 50, então a tabela "
                    + "inteira nunca é devolvida de uma vez.")
    @ApiResponse(responseCode = "200", description = "Página de jogadores")
    @RespostaDadosInvalidos
    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @Parameter(description = "Posição em quadra")
            @RequestParam(required = false) Posicao posicao,
            @Parameter(description = "Busca parcial por nome completo ou nickname", example = "romao")
            @RequestParam(required = false) String busca,
            @Parameter(description = "Nacionalidade do jogador", example = "Brasileiro")
            @RequestParam(required = false) String nacionalidade,
            @ParameterObject
            @PageableDefault(size = 10, sort = "nomeCompleto", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(posicao, busca, nacionalidade, pageable));
    }

    @Operation(operationId = "buscarUsuario", summary = "Busca um jogador por id")
    @ApiResponse(responseCode = "200", description = "Jogador encontrado")
    @RespostaNaoEncontrado
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @Operation(operationId = "buscarUsuarioPorNickname", summary = "Busca um jogador pelo nickname")
    @ApiResponse(responseCode = "200", description = "Jogador encontrado")
    @RespostaNaoEncontrado
    @GetMapping("/nickname/{nickname}")
    public ResponseEntity<UsuarioResponse> buscarPorNickname(@PathVariable String nickname) {
        return ResponseEntity.ok(usuarioService.buscarPorNickname(nickname));
    }

    @Operation(operationId = "atualizarUsuario", summary = "Atualiza o perfil",
            description = "Cada jogador só altera o próprio perfil. A senha não é alterada por aqui.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                     @RequestBody @Valid UsuarioRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request, UsuarioLogado.id(jwt)));
    }

    @Operation(operationId = "deletarUsuario", summary = "Apaga o perfil", description = "Cada jogador só apaga o próprio perfil.")
    @ApiResponse(responseCode = "204", description = "Perfil apagado")
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.deletar(id, UsuarioLogado.id(jwt));
    }
}
