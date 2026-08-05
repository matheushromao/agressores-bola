package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.dto.EstatisticaRequest;
import com.hmz.agressores_da_bola.dto.EstatisticaResponse;
import com.hmz.agressores_da_bola.service.EstatisticaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Súmula da pelada. Fica sob {@code /api/peladas} porque estatística sem
 * pelada não existe, mas em um controller separado para não misturar a
 * gestão do jogo com a gestão dos números.
 */
@RestController
@RequestMapping("/api/peladas/{peladaId}")
@RequiredArgsConstructor
public class EstatisticaController {

    private final EstatisticaService estatisticaService;

    /**
     * Súmula completa da pelada, já ordenada por pontuação.
     */
    @GetMapping("/estatisticas")
    public ResponseEntity<List<EstatisticaResponse>> listar(@PathVariable Long peladaId) {
        return ResponseEntity.ok(estatisticaService.listarDaPelada(peladaId));
    }

    @GetMapping("/participantes/{usuarioId}/estatistica")
    public ResponseEntity<EstatisticaResponse> buscar(@PathVariable Long peladaId,
                                                      @PathVariable Long usuarioId) {
        return ResponseEntity.ok(estatisticaService.buscar(peladaId, usuarioId));
    }

    /**
     * PUT e não POST: a súmula de um jogador na pelada é única, então lançar
     * de novo corrige o lançamento anterior em vez de criar outro.
     */
    @PutMapping("/participantes/{usuarioId}/estatistica")
    public ResponseEntity<EstatisticaResponse> registrar(@PathVariable Long peladaId,
                                                         @PathVariable Long usuarioId,
                                                         @RequestBody @Valid EstatisticaRequest request) {
        return ResponseEntity.ok(estatisticaService.registrar(peladaId, usuarioId, request));
    }

    @DeleteMapping("/participantes/{usuarioId}/estatistica")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long peladaId, @PathVariable Long usuarioId) {
        estatisticaService.remover(peladaId, usuarioId);
    }
}
