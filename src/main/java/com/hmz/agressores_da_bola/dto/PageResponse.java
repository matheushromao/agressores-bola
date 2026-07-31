package com.hmz.agressores_da_bola.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação da API. Existe para não expor o contrato interno do
 * {@link Page} do Spring Data (que muda entre versões e carrega metadados
 * desnecessários) e para manter uma resposta estável para o front.
 *
 * @param <T> tipo do DTO de resposta que compõe a página
 */
public record PageResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    public static <T> PageResponse<T> de(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Converte uma página de entidades em uma página de DTOs, evitando que
     * cada service repita o {@code page.map(...)} seguido do {@code de(...)}.
     */
    public static <E, T> PageResponse<T> de(Page<E> page, Function<E, T> conversor) {
        return de(page.map(conversor));
    }
}
