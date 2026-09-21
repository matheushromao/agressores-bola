package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.config.openapi.RespostaDadosInvalidos;
import com.hmz.agressores_da_bola.config.openapi.RespostaNaoEncontrado;
import com.hmz.agressores_da_bola.config.openapi.RespostaRegraDeNegocio;
import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.ParticipacaoRequest;
import com.hmz.agressores_da_bola.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.dto.PeladaFiltro;
import com.hmz.agressores_da_bola.dto.PeladaRequest;
import com.hmz.agressores_da_bola.dto.PeladaResponse;
import com.hmz.agressores_da_bola.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.dto.StatusParticipacaoRequest;
import com.hmz.agressores_da_bola.dto.StatusPeladaRequest;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;
import com.hmz.agressores_da_bola.security.UsuarioLogado;
import com.hmz.agressores_da_bola.service.PeladaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/peladas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Peladas", description = "Agendamento das partidas e escalação dos jogadores")
public class PeladaController {

    private final PeladaService peladaService;

    /* ------------------------------------------------------------------
     * Pelada
     * ------------------------------------------------------------------ */

    /**
     * O organizador é sempre quem está logado — não vem no corpo.
     */
    @Operation(operationId = "criarPelada", summary = "Agenda uma pelada",
            description = "O organizador é sempre o usuário autenticado: não vem no corpo da "
                    + "requisição e por isso não pode ser forjado.")
    @ApiResponse(responseCode = "201", description = "Pelada criada; o Location aponta para ela")
    @RespostaDadosInvalidos
    @PostMapping
    public ResponseEntity<PeladaResponse> criar(@RequestBody @Valid PeladaRequest request,
                                                @AuthenticationPrincipal Jwt jwt,
                                                UriComponentsBuilder uriBuilder) {
        PeladaResponse response = peladaService.criar(request, UsuarioLogado.id(jwt));
        URI location = uriBuilder.path("/api/peladas/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Listagem paginada com filtros opcionais:
     * {@code ?cidade=Sorocaba&status=AGENDADA&page=0&size=10&sort=data,asc}.
     */
    @Operation(operationId = "listarPeladas", summary = "Lista peladas com filtros e paginação",
            description = "Endpoint público. Os filtros são combináveis e todos opcionais; sem "
                    + "ordenação explícita o resultado vem por data e hora de início.")
    @ApiResponse(responseCode = "200", description = "Página de peladas")
    @RespostaDadosInvalidos
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<PageResponse<PeladaResumoResponse>> listar(
            @Parameter(description = "Situação da pelada")
            @RequestParam(required = false) StatusPelada status,
            @Parameter(description = "Tipo de campo")
            @RequestParam(required = false) TipoCampo tipoCampo,
            @Parameter(description = "Cidade; busca parcial, sem diferenciar maiúsculas", example = "Sorocaba")
            @RequestParam(required = false) String cidade,
            @Parameter(description = "Peladas a partir desta data (ISO)", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @Parameter(description = "Peladas até esta data (ISO)", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @Parameter(description = "Só as peladas organizadas por este jogador")
            @RequestParam(required = false) Long organizadorId,
            @Parameter(description = "Só as peladas em que este jogador está escalado")
            @RequestParam(required = false) Long participanteId,
            @ParameterObject
            @PageableDefault(size = 10, sort = {"data", "horaInicio"}, direction = Sort.Direction.ASC) Pageable pageable) {

        PeladaFiltro filtro = new PeladaFiltro(
                status, tipoCampo, cidade, dataInicial, dataFinal, organizadorId, participanteId);
        return ResponseEntity.ok(peladaService.listar(filtro, pageable));
    }

    @Operation(operationId = "buscarPelada", summary = "Detalha uma pelada", description = "Endpoint público.")
    @ApiResponse(responseCode = "200", description = "Pelada encontrada")
    @RespostaNaoEncontrado
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<PeladaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(peladaService.buscarPorId(id));
    }

    @Operation(operationId = "atualizarPelada", summary = "Atualiza os dados da pelada",
            description = "Só o organizador da pelada pode alterá-la.")
    @ApiResponse(responseCode = "200", description = "Pelada atualizada")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PutMapping("/{id}")
    public ResponseEntity<PeladaResponse> atualizar(@PathVariable Long id,
                                                    @RequestBody @Valid PeladaRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(peladaService.atualizar(id, request, UsuarioLogado.id(jwt)));
    }

    @Operation(operationId = "alterarStatusPelada", summary = "Altera a situação da pelada",
            description = "Transições aceitas: AGENDADA para EM_ANDAMENTO, EM_ANDAMENTO para "
                    + "FINALIZADA, e CANCELADA a partir das duas primeiras.")
    @ApiResponse(responseCode = "200", description = "Situação alterada")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PatchMapping("/{id}/status")
    public ResponseEntity<PeladaResponse> alterarStatus(@PathVariable Long id,
                                                        @RequestBody @Valid StatusPeladaRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(peladaService.alterarStatus(id, request.status(), UsuarioLogado.id(jwt)));
    }

    @Operation(operationId = "deletarPelada", summary = "Apaga a pelada", description = "Só o organizador pode apagar.")
    @ApiResponse(responseCode = "204", description = "Pelada apagada")
    @RespostaNaoEncontrado
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        peladaService.deletar(id, UsuarioLogado.id(jwt));
    }

    /* ------------------------------------------------------------------
     * Escalação (o "grupo" da pelada)
     * ------------------------------------------------------------------ */

    @Operation(operationId = "listarParticipantes", summary = "Lista a escalação",
            description = "Endpoint público. Traz convidados, confirmados e a lista de espera.")
    @ApiResponse(responseCode = "200", description = "Escalação da pelada")
    @RespostaNaoEncontrado
    @SecurityRequirements
    @GetMapping("/{id}/participantes")
    public ResponseEntity<List<ParticipanteResponse>> listarParticipantes(@PathVariable Long id) {
        return ResponseEntity.ok(peladaService.listarParticipantes(id));
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
        ParticipanteResponse response = peladaService.adicionarParticipante(id, request, UsuarioLogado.id(jwt));
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
        return ResponseEntity.ok(peladaService.alterarStatusParticipacao(
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
        peladaService.removerParticipante(id, usuarioId, UsuarioLogado.id(jwt));
    }
}
