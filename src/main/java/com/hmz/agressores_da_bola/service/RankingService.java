package com.hmz.agressores_da_bola.service;

import com.hmz.agressores_da_bola.dto.DestaqueResponse;
import com.hmz.agressores_da_bola.dto.RankingAtributoResponse;
import com.hmz.agressores_da_bola.dto.RankingResponse;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;

import java.util.List;

/**
 * Contrato dos rankings. Todos aceitam um {@code peladaId} opcional: nulo
 * significa a classificação de todos os tempos, preenchido restringe a uma
 * pelada específica.
 */
public interface RankingService {

    /**
     * Classificação geral por pontos, do primeiro ao último colocado.
     */
    List<RankingResponse> geral(Long peladaId, Integer limite);

    /**
     * Artilharia, garçons, desarmes, defesas ou defesas difíceis, conforme o
     * atributo pedido.
     */
    List<RankingAtributoResponse> porAtributo(AtributoPontuacao atributo, Long peladaId, Integer limite);

    /**
     * Os rankings de todos os atributos de uma vez, para a tela de destaques.
     */
    List<DestaqueResponse> destaques(Long peladaId, Integer limite);
}
