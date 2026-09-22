package com.hmz.agressores_da_bola.web.controller;

import com.hmz.agressores_da_bola.application.pelada.EscalacaoService;
import com.hmz.agressores_da_bola.application.pelada.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.web.dto.ParticipacaoRequest;
import com.hmz.agressores_da_bola.web.dto.StatusParticipacaoRequest;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Escalação (o "grupo" da pelada). Fica sob {@code /api/peladas} e na mesma
 * tag do Swagger porque escalação sem pelada não existe, mas em um controller
 * separado para não misturar o agendamento com a gestão do elenco.
 */
@RestController
@RequestMapping(value = "/api/peladas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Peladas", description = "Agendamento das partidas e escalação dos jogadores")
public class EscalacaoController {

    private final EscalacaoService escalacaoService;

    @Operation(operationId = "listarParticipantes", summary = "Lista a escalação",
            description = "Endpoint público. Traz convidados, confirmados e a lista de espera.")
    @ApiResponse(responseCode = "200", description = "Escalação da pelada")
    @RespostaNaoEncontrado
    @SecurityRequirements
    @GetMapping("/{id}/participantes")
    public ResponseEntity<List<ParticipanteResponse>> listarParticipantes(@PathVariable Long id) {
        return ResponseEntity.ok(escalacaoService.listar(id));
    }

    @Operation(operationId = "adicionarParticipante", summary = "Escala um jogador",
            description = "Sem vaga livre o jogador entra na lista de espera e é promovido "
                    + "automaticamente quando alguém desiste.")
    @ApiResponse(responseCode = "201", description = "Jogador escalado ou posto em espera")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PostMapping("/{id}/participantes")
    public ResponseEntity<ParticipanteResponse> adicionarParticipante(
            @PathVariable Long id,
            @RequestBody @Valid ParticipacaoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            UriComponentsBuilder uriBuilder) {
        ParticipanteResponse response = escalacaoService.adicionar(
                id, request.usuarioId(), request.statusOuPadrao(), UsuarioLogado.id(jwt));
        URI location = uriBuilder.path("/api/peladas/{id}/participantes")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(operationId = "alterarStatusParticipacao", summary = "Altera a participação de um jogador",
            description = "O próprio jogador muda a sua participação; o organizador muda a de qualquer um.")
    @ApiResponse(responseCode = "200", description = "Participação atualizada")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PatchMapping("/{id}/participantes/{usuarioId}")
    public ResponseEntity<ParticipanteResponse> alterarStatusParticipacao(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @RequestBody @Valid StatusParticipacaoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(escalacaoService.alterarStatus(
                id, usuarioId, request.status(), UsuarioLogado.id(jwt)));
    }

    @Operation(operationId = "removerParticipante", summary = "Remove um jogador da escalação",
            description = "Libera a vaga e promove o primeiro da lista de espera, se houver.")
    @ApiResponse(responseCode = "204", description = "Jogador removido")
    @RespostaNaoEncontrado
    @DeleteMapping("/{id}/participantes/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerParticipante(@PathVariable Long id, @PathVariable Long usuarioId,
                                    @AuthenticationPrincipal Jwt jwt) {
        escalacaoService.remover(id, usuarioId, UsuarioLogado.id(jwt));
    }
}
