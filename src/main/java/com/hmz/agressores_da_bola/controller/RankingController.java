package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.dto.DestaqueResponse;
import com.hmz.agressores_da_bola.dto.RankingAtributoResponse;
import com.hmz.agressores_da_bola.dto.RankingResponse;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rankings da liga. Todos os endpoints aceitam {@code ?peladaId=} para
 * restringir a classificação a uma pelada e {@code ?limite=} para trazer só
 * o topo da lista.
 */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * Classificação geral por pontos, do primeiro ao último colocado.
     */
    @GetMapping
    public ResponseEntity<List<RankingResponse>> geral(
            @RequestParam(required = false) Long peladaId,
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(rankingService.geral(peladaId, limite));
    }

    /**
     * Ranking de um atributo específico:
     * {@code /api/ranking/atributos/GOL}, {@code .../DEFESA_DIFICIL}.
     */
    @GetMapping("/atributos/{atributo}")
    public ResponseEntity<List<RankingAtributoResponse>> porAtributo(
            @PathVariable AtributoPontuacao atributo,
            @RequestParam(required = false) Long peladaId,
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(rankingService.porAtributo(atributo, peladaId, limite));
    }

    /**
     * Os artilheiros, os garçons e os paredões de uma vez só.
     */
    @GetMapping("/destaques")
    public ResponseEntity<List<DestaqueResponse>> destaques(
            @RequestParam(required = false) Long peladaId,
            @RequestParam(defaultValue = "5") Integer limite) {
        return ResponseEntity.ok(rankingService.destaques(peladaId, limite));
    }
}
