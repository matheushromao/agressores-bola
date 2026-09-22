package com.hmz.agressores_da_bola.web.controller;

import com.hmz.agressores_da_bola.application.sorteio.SorteioService;
import com.hmz.agressores_da_bola.application.sorteio.dto.SorteioResponse;
import com.hmz.agressores_da_bola.web.dto.SorteioRequest;
import com.hmz.agressores_da_bola.web.openapi.RespostaDadosInvalidos;
import com.hmz.agressores_da_bola.web.openapi.RespostaNaoEncontrado;
import com.hmz.agressores_da_bola.web.openapi.RespostaRegraDeNegocio;
import com.hmz.agressores_da_bola.web.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/peladas/{peladaId}/sorteio", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Sorteio", description = "Divisão dos confirmados em times equilibrados por estrelas")
public class SorteioController {

    private final SorteioService sorteioService;

    /**
     * POST mesmo sem gravar nada: cada chamada produz uma divisão diferente,
     * então a operação não é idempotente como um GET precisaria ser.
     */
    @Operation(operationId = "sortearTimes", summary = "Sorteia os times da pelada",
            description = "Divide os jogadores confirmados equilibrando a soma de estrelas e "
                    + "distribuindo um goleiro por time. Nada é gravado: cada chamada produz "
                    + "uma divisão diferente, e é por isso que o verbo é POST e não GET.")
    @ApiResponse(responseCode = "200", description = "Times sorteados")
    @RespostaDadosInvalidos
    @RespostaNaoEncontrado
    @RespostaRegraDeNegocio
    @PostMapping
    public ResponseEntity<SorteioResponse> sortear(@PathVariable Long peladaId,
                                                   @RequestBody @Valid SorteioRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(sorteioService.sortear(peladaId, request.paraCriterio(), UsuarioLogado.id(jwt)));
    }
}
