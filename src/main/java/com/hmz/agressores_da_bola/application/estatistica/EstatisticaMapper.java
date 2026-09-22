package com.hmz.agressores_da_bola.application.estatistica;

import com.hmz.agressores_da_bola.application.estatistica.dto.EstatisticaResponse;
import com.hmz.agressores_da_bola.application.estatistica.dto.PontuacaoAtributoResponse;
import com.hmz.agressores_da_bola.application.usuario.UsuarioMapper;
import com.hmz.agressores_da_bola.domain.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.domain.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.hmz.agressores_da_bola.domain.model.enums.Descritivel.descricaoDe;

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
                descricaoDe(estatistica.getPosicaoJogada()),
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
