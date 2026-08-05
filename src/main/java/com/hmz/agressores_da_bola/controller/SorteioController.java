package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.dto.SorteioRequest;
import com.hmz.agressores_da_bola.dto.SorteioResponse;
import com.hmz.agressores_da_bola.service.SorteioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peladas/{peladaId}/sorteio")
@RequiredArgsConstructor
public class SorteioController {

    private final SorteioService sorteioService;

    /**
     * POST mesmo sem gravar nada: cada chamada produz uma divisão diferente,
     * então a operação não é idempotente como um GET precisaria ser.
     */
    @PostMapping
    public ResponseEntity<SorteioResponse> sortear(@PathVariable Long peladaId,
                                                   @RequestBody @Valid SorteioRequest request) {
        return ResponseEntity.ok(sorteioService.sortear(peladaId, request));
    }
}
