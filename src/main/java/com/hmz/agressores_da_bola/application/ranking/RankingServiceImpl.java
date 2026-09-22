package com.hmz.agressores_da_bola.application.ranking;

import com.hmz.agressores_da_bola.application.port.EstatisticaRepository;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.application.ranking.dto.DestaqueResponse;
import com.hmz.agressores_da_bola.application.ranking.dto.RankingAtributoResponse;
import com.hmz.agressores_da_bola.application.ranking.dto.RankingResponse;
import com.hmz.agressores_da_bola.domain.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.domain.ranking.Classificacao;
import com.hmz.agressores_da_bola.domain.ranking.OrdenacaoDoRanking;
import com.hmz.agressores_da_bola.domain.ranking.TotaisJogador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Busca os totais e monta as tabelas. Como ordenar e como distribuir as
 * colocações com empate são regras do domínio ({@link OrdenacaoDoRanking} e
 * {@link Classificacao}).
 */
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final EstatisticaRepository estatisticaRepository;
    private final PeladaRepository peladaRepository;
    private final RankingMapper rankingMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> geral(Long peladaId, Integer limite) {
        return Classificacao.classificar(
                somarPorJogador(peladaId),
                OrdenacaoDoRanking.geral(),
                TotaisJogador::pontuacao,
                limite,
                rankingMapper::toRankingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingAtributoResponse> porAtributo(AtributoPontuacao atributo, Long peladaId, Integer limite) {
        return rankingDe(atributo, somarPorJogador(peladaId), limite);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestaqueResponse> destaques(Long peladaId, Integer limite) {
        // Uma única leitura do banco alimenta o ranking de todos os atributos.
        List<TotaisJogador> totais = somarPorJogador(peladaId);

        return Arrays.stream(AtributoPontuacao.values())
                .map(atributo -> new DestaqueResponse(
                        atributo,
                        atributo.getDescricao(),
                        atributo.getPeso(),
                        rankingDe(atributo, totais, limite)))
                .toList();
    }

    private List<TotaisJogador> somarPorJogador(Long peladaId) {
        if (peladaId != null) {
            peladaRepository.garantirQueExiste(peladaId);
        }
        return estatisticaRepository.somarPorJogador(peladaId);
    }

    private List<RankingAtributoResponse> rankingDe(AtributoPontuacao atributo,
                                                    List<TotaisJogador> totais,
                                                    Integer limite) {
        List<TotaisJogador> pontuadores = totais.stream()
                .filter(linha -> linha.pontuouEm(atributo))
                .toList();

        return Classificacao.classificar(
                pontuadores,
                OrdenacaoDoRanking.porAtributo(atributo),
                linha -> linha.quantidadeDe(atributo),
                limite,
                (linha, posicao) -> rankingMapper.toRankingAtributoResponse(linha, atributo, posicao));
    }
}
