package com.hmz.agressores_da_bola.mapper;

import com.hmz.agressores_da_bola.dto.EstatisticaResponse;
import com.hmz.agressores_da_bola.dto.PontuacaoAtributoResponse;
import com.hmz.agressores_da_bola.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EstatisticaMapper {

    private final UsuarioMapper usuarioMapper;

    public EstatisticaResponse toResponse(EstatisticaPartida estatistica) {
        ResumoEstatistico resumo = estatistica.resumo();

        return new EstatisticaResponse(
                estatistica.getId(),
                estatistica.getPelada() != null ? estatistica.getPelada().getId() : null,
                usuarioMapper.toResumoResponse(estatistica.getJogador()),
                estatistica.getPosicaoJogada(),
                estatistica.getPosicaoJogada() != null ? estatistica.getPosicaoJogada().getDescricao() : null,
                estatistica.jogouNoGol(),
                resumo.gols(),
                resumo.assistencias(),
                resumo.desarmes(),
                resumo.defesas(),
                resumo.defesasDificeis(),
                resumo.pontuacao(),
                detalhar(resumo),
                estatistica.getRegistradaEm(),
                estatistica.getAtualizadaEm()
        );
    }

    /**
     * Abre a conta da pontuação atributo por atributo. Atributos zerados
     * ficam de fora para a resposta do goleiro não vir cheia de desarmes 0
     * (e vice-versa).
     */
    public List<PontuacaoAtributoResponse> detalhar(ResumoEstatistico resumo) {
        return Arrays.stream(AtributoPontuacao.values())
                .filter(atributo -> atributo.quantidade(resumo) > 0)
                .map(atributo -> new PontuacaoAtributoResponse(
                        atributo,
                        atributo.getDescricao(),
                        atributo.quantidade(resumo),
                        atributo.getPeso(),
                        atributo.pontos(resumo)))
                .toList();
    }
}
