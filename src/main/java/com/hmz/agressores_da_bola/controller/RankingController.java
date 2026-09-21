package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.config.openapi.RespostaDadosInvalidos;
import com.hmz.agressores_da_bola.dto.DestaqueResponse;
import com.hmz.agressores_da_bola.dto.RankingAtributoResponse;
import com.hmz.agressores_da_bola.dto.RankingResponse;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
@RequestMapping(value = "/api/ranking", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Rankings",
        description = "Classificações agregadas da liga. Todos os endpoints são públicos.")
public class RankingController {

    private final RankingService rankingService;

    /**
     * Classificação geral por pontos, do primeiro ao último colocado.
     */
    @Operation(operationId = "rankingGeral", summary = "Classificação geral por pontos",
            description = "Do primeiro ao último colocado, somando a tabela de pontuação de "
                    + "todas as súmulas lançadas.")
    @ApiResponse(responseCode = "200", description = "Classificação geral")
    @RespostaDadosInvalidos
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<RankingResponse>> geral(
            @Parameter(description = "Restringe a classificação a uma pelada")
            @RequestParam(required = false) Long peladaId,
            @Parameter(description = "Traz apenas os N primeiros colocados", example = "10")
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(rankingService.geral(peladaId, limite));
    }

    /**
     * Ranking de um atributo específico:
     * {@code /api/ranking/atributos/GOL}, {@code .../DEFESA_DIFICIL}.
     */
    @Operation(operationId = "rankingPorAtributo", summary = "Ranking de um atributo",
            description = "Artilharia (GOL), assistências, desarmes, defesas e defesas difíceis.")
    @ApiResponse(responseCode = "200", description = "Ranking do atributo")
    @RespostaDadosInvalidos
    @SecurityRequirements
    @GetMapping("/atributos/{atributo}")
    public ResponseEntity<List<RankingAtributoResponse>> porAtributo(
            @Parameter(description = "Atributo da tabela de pontuação", example = "GOL")
            @PathVariable AtributoPontuacao atributo,
            @Parameter(description = "Restringe o ranking a uma pelada")
            @RequestParam(required = false) Long peladaId,
            @Parameter(description = "Traz apenas os N primeiros colocados", example = "10")
            @RequestParam(required = false) Integer limite) {
        return ResponseEntity.ok(rankingService.porAtributo(atributo, peladaId, limite));
    }

    /**
     * Os artilheiros, os garçons e os paredões de uma vez só.
     */
    @Operation(operationId = "destaques", summary = "Destaques por atributo",
            description = "Os artilheiros, os garçons e os paredões em uma única chamada.")
    @ApiResponse(responseCode = "200", description = "Destaques de cada atributo")
    @RespostaDadosInvalidos
    @SecurityRequirements
    @GetMapping("/destaques")
    public ResponseEntity<List<DestaqueResponse>> destaques(
            @Parameter(description = "Restringe os destaques a uma pelada")
            @RequestParam(required = false) Long peladaId,
            @Parameter(description = "Quantos destaques por atributo", example = "5")
            @RequestParam(defaultValue = "5") Integer limite) {
        return ResponseEntity.ok(rankingService.destaques(peladaId, limite));
    }
}
