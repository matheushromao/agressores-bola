package com.hmz.agressores_da_bola.application.ranking;

import com.hmz.agressores_da_bola.application.estatistica.EstatisticaMapper;
import com.hmz.agressores_da_bola.application.ranking.dto.RankingAtributoResponse;
import com.hmz.agressores_da_bola.application.ranking.dto.RankingResponse;
import com.hmz.agressores_da_bola.application.usuario.dto.UsuarioResumoResponse;
import com.hmz.agressores_da_bola.domain.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.domain.ranking.TotaisJogador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.hmz.agressores_da_bola.domain.model.enums.Descritivel.descricaoDe;

/**
 * Conversão das linhas agregadas do banco em linhas de ranking. Não passa
 * pelo {@code UsuarioMapper} porque aqui não existe entidade carregada: os
 * dados do jogador vêm da própria projeção, sem ida extra ao banco.
 */
@Component
@RequiredArgsConstructor
public class RankingMapper {

    private final EstatisticaMapper estatisticaMapper;

    public RankingResponse toRankingResponse(TotaisJogador totais, int posicao) {
        ResumoEstatistico resumo = totais.resumo();
        int pontuacao = totais.pontuacao();

        return new RankingResponse(
                posicao,
                toJogador(totais),
                totais.jogos(),
                totais.gols(),
                totais.assistencias(),
                totais.desarmes(),
                totais.defesas(),
                totais.defesasDificeis(),
                pontuacao,
                mediaPorJogo(pontuacao, totais.jogos()),
                estatisticaMapper.detalhar(resumo)
        );
    }

    public RankingAtributoResponse toRankingAtributoResponse(TotaisJogador totais,
                                                             AtributoPontuacao atributo,
                                                             int posicao) {
        ResumoEstatistico resumo = totais.resumo();

        return new RankingAtributoResponse(
                posicao,
                toJogador(totais),
                totais.jogos(),
                atributo.quantidade(resumo),
                atributo.pontos(resumo)
        );
    }

    private UsuarioResumoResponse toJogador(TotaisJogador totais) {
        return new UsuarioResumoResponse(
                totais.usuarioId(),
                totais.nickname(),
                totais.nomeCompleto(),
                totais.posicao(),
                descricaoDe(totais.posicao()),
                totais.estrelas() != null ? totais.estrelas() : Usuario.ESTRELAS_PADRAO
        );
    }

    /**
     * Média arredondada em duas casas: serve para comparar quem rende mais
     * por jogo com quem só somou pontos por ter aparecido mais vezes.
     */
    private double mediaPorJogo(int pontuacao, long jogos) {
        if (jogos <= 0) {
            return 0d;
        }
        return Math.round((double) pontuacao / jogos * 100d) / 100d;
    }
}
