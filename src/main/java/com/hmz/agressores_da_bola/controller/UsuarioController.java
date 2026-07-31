package com.hmz.agressores_da_bola.controller;

import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.UsuarioRequest;
import com.hmz.agressores_da_bola.dto.UsuarioResponse;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        UsuarioResponse response = usuarioService.criar(request);
        URI location = uriBuilder.path("/api/usuarios/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Listagem paginada: {@code ?page=0&size=10&sort=nomeCompleto,asc}.
     * O @PageableDefault define o comportamento quando o cliente não manda
     * nada, então o backend nunca devolve a tabela inteira.
     */
    @GetMapping
    public ResponseEntity<PageResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) Posicao posicao,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String nacionalidade,
            @PageableDefault(size = 10, sort = "nomeCompleto", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(posicao, busca, nacionalidade, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping("/nickname/{nickname}")
    public ResponseEntity<UsuarioResponse> buscarPorNickname(@PathVariable String nickname) {
        return ResponseEntity.ok(usuarioService.buscarPorNickname(nickname));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                     @RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
    }
}
