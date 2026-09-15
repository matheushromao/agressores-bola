package com.hmz.agressores_da_bola.controller;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/peladas")
@RequiredArgsConstructor
public class PeladaController {

    private final PeladaService peladaService;

    /* ------------------------------------------------------------------
     * Pelada
     * ------------------------------------------------------------------ */

    /**
     * O organizador é sempre quem está logado — não vem no corpo.
     */
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
    @GetMapping
    public ResponseEntity<PageResponse<PeladaResumoResponse>> listar(
            @RequestParam(required = false) StatusPelada status,
            @RequestParam(required = false) TipoCampo tipoCampo,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) Long organizadorId,
            @RequestParam(required = false) Long participanteId,
            @PageableDefault(size = 10, sort = {"data", "horaInicio"}, direction = Sort.Direction.ASC) Pageable pageable) {

        PeladaFiltro filtro = new PeladaFiltro(
                status, tipoCampo, cidade, dataInicial, dataFinal, organizadorId, participanteId);
        return ResponseEntity.ok(peladaService.listar(filtro, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeladaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(peladaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeladaResponse> atualizar(@PathVariable Long id,
                                                    @RequestBody @Valid PeladaRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(peladaService.atualizar(id, request, UsuarioLogado.id(jwt)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PeladaResponse> alterarStatus(@PathVariable Long id,
                                                        @RequestBody @Valid StatusPeladaRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(peladaService.alterarStatus(id, request.status(), UsuarioLogado.id(jwt)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        peladaService.deletar(id, UsuarioLogado.id(jwt));
    }

    /* ------------------------------------------------------------------
     * Escalação (o "grupo" da pelada)
     * ------------------------------------------------------------------ */

    @GetMapping("/{id}/participantes")
    public ResponseEntity<List<ParticipanteResponse>> listarParticipantes(@PathVariable Long id) {
        return ResponseEntity.ok(peladaService.listarParticipantes(id));
    }

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

    @PatchMapping("/{id}/participantes/{usuarioId}")
    public ResponseEntity<ParticipanteResponse> alterarStatusParticipacao(
            @PathVariable Long id,
            @PathVariable Long usuarioId,
            @RequestBody @Valid StatusParticipacaoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(peladaService.alterarStatusParticipacao(
                id, usuarioId, request.status(), UsuarioLogado.id(jwt)));
    }

    @DeleteMapping("/{id}/participantes/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerParticipante(@PathVariable Long id, @PathVariable Long usuarioId,
                                    @AuthenticationPrincipal Jwt jwt) {
        peladaService.removerParticipante(id, usuarioId, UsuarioLogado.id(jwt));
    }
}
