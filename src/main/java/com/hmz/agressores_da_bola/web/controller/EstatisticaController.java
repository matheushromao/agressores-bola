package com.hmz.agressores_da_bola.web.controller;

import com.hmz.agressores_da_bola.application.estatistica.EstatisticaService;
import com.hmz.agressores_da_bola.application.estatistica.dto.EstatisticaResponse;
import com.hmz.agressores_da_bola.web.dto.EstatisticaRequest;
import com.hmz.agressores_da_bola.web.openapi.RespostaDadosInvalidos;
import com.hmz.agressores_da_bola.web.openapi.RespostaNaoEncontrado;
import com.hmz.agressores_da_bola.web.openapi.RespostaRegraDeNegocio;
import com.hmz.agressores_da_bola.web.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Súmula da pelada. Fica sob {@code /api/peladas} porque estatística sem
 * pelada não existe, mas em um controller separado para não misturar a
 * gestão do jogo com a gestão dos números.
 */
@RestController
@RequestMapping(value = "/api/peladas/{peladaId}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Estatísticas",
        description = "Súmula da partida: gols, assistências, desarmes e defesas por jogador. "
                + "Leitura pública; o lançamento é do organizador.")
public class EstatisticaController {

    private final EstatisticaService estatisticaService;

    /**
     * Súmula completa da pelada, já ordenada por pontuação.
     */
    @Operation(operationId = "listarEstatisticas", summary = "Lista a súmula da pelada",
            description = "Endpoint público, já ordenado por pontuação.")
    @ApiResponse(responseCode = "200", description = "Súmula da pelada")
    @RespostaNaoEncontrado
    @SecurityRequirements
    @GetMapping("/estatisticas")
    public ResponseEntity<List<EstatisticaResponse>> listar(@PathVariable Long peladaId) {
        return ResponseEntity.ok(estatisticaService.listarDaPelada(peladaId));
    }

    @Operation(operationId = "buscarEstatistica", summary = "Busca a súmula de um jogador na pelada",
            description = "Endpoint público.")
    @ApiResponse(responseCode = "200", description = "Súmula do jogador")
    @RespostaNaoEncontrado
    @SecurityRequirements
    @GetMapping("/participantes/{usuarioId}/estatistica")
    public ResponseEntity<EstatisticaResponse> buscar(@PathVariable Long peladaId,
                                                      @PathVariable Long usuarioId) {
        return ResponseEntity.ok(estatisticaService.buscar(peladaId, usuarioId));
    }

    /**
     * PUT e não POST: a súmula de um jogador na pelada é única, então lançar
     * de novo corrige o lançamento anterior em vez de criar outro.
     */
    @Operation(operationId = "registrarEstatistica", summary = "Lança ou corrige a súmula de um jogador",
            description = "PUT e não POST: a súmula é única por jogador na pelada, então lançar "
                    + "de novo corrige o lançamento anterior. Atributos de goleiro só valem para "
                    + "quem está escalado no gol.")
    @ApiResponse(responseCode = "200", description = "Súmula lançada")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PutMapping("/participantes/{usuarioId}/estatistica")
    public ResponseEntity<EstatisticaResponse> registrar(@PathVariable Long peladaId,
                                                         @PathVariable Long usuarioId,
                                                         @RequestBody @Valid EstatisticaRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(estatisticaService.registrar(
                peladaId, usuarioId, request.paraLancamento(), UsuarioLogado.id(jwt)));
    }

    @Operation(operationId = "removerEstatistica", summary = "Apaga a súmula de um jogador")
    @ApiResponse(responseCode = "204", description = "Súmula apagada")
    @RespostaNaoEncontrado
    @DeleteMapping("/participantes/{usuarioId}/estatistica")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long peladaId, @PathVariable Long usuarioId,
                        @AuthenticationPrincipal Jwt jwt) {
        estatisticaService.remover(peladaId, usuarioId, UsuarioLogado.id(jwt));
    }
}
