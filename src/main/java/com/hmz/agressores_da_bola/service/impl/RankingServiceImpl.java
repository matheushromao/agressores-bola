package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.DestaqueResponse;
import com.hmz.agressores_da_bola.dto.RankingAtributoResponse;
import com.hmz.agressores_da_bola.dto.RankingResponse;
import com.hmz.agressores_da_bola.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.mapper.RankingMapper;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.repository.EstatisticaPartidaRepository;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import com.hmz.agressores_da_bola.repository.projection.TotaisJogador;
import com.hmz.agressores_da_bola.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final EstatisticaPartidaRepository estatisticaRepository;
    private final PeladaRepository peladaRepository;
    private final RankingMapper rankingMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RankingResponse> geral(Long peladaId, Integer limite) {
        return classificar(
                somarPorJogador(peladaId),
                ordenacaoGeral(),
                totais -> totais.resumo().pontuacao(),
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

    /* ------------------------------------------------------------------
     * Montagem da classificação
     * ------------------------------------------------------------------ */

    private List<TotaisJogador> somarPorJogador(Long peladaId) {
        if (peladaId != null && !peladaRepository.existsById(peladaId)) {
            throw new RecursoNaoEncontradoException("Pelada não encontrada com o id: " + peladaId);
        }
        return estatisticaRepository.somarPorJogador(peladaId);
    }

    private List<RankingAtributoResponse> rankingDe(AtributoPontuacao atributo,
                                                    List<TotaisJogador> totais,
                                                    Integer limite) {
        // Quem não marcou não entra na artilharia: uma lista de artilheiros
        // com zero gols só polui a tela.
        List<TotaisJogador> pontuadores = totais.stream()
                .filter(linha -> atributo.quantidade(linha.resumo()) > 0)
                .toList();

        return classificar(
                pontuadores,
                ordenacaoPorAtributo(atributo),
                linha -> atributo.quantidade(linha.resumo()),
                limite,
                (linha, posicao) -> rankingMapper.toRankingAtributoResponse(linha, atributo, posicao));
    }

    /**
     * Ordena, corta pelo limite e distribui as colocações. Empate divide a
     * posição e pula a seguinte (1, 2, 2, 4), como em qualquer tabela.
     */
    private <T> List<T> classificar(List<TotaisJogador> linhas,
                                    Comparator<TotaisJogador> ordenacao,
                                    ToIntFunction<TotaisJogador> criterioDeEmpate,
                                    Integer limite,
                                    BiFunction<TotaisJogador, Integer, T> montador) {

        List<TotaisJogador> ordenadas = linhas.stream()
                .sorted(ordenacao)
                .limit(limiteEfetivo(limite, linhas.size()))
                .toList();

        List<T> classificacao = new ArrayList<>(ordenadas.size());
        int posicao = 0;
        Integer valorAnterior = null;

        for (int indice = 0; indice < ordenadas.size(); indice++) {
            TotaisJogador linha = ordenadas.get(indice);
            int valor = criterioDeEmpate.applyAsInt(linha);

            if (valorAnterior == null || valor != valorAnterior) {
                posicao = indice + 1;
                valorAnterior = valor;
            }
            classificacao.add(montador.apply(linha, posicao));
        }
        return classificacao;
    }

    private long limiteEfetivo(Integer limite, int total) {
        return limite == null || limite <= 0 ? total : limite;
    }

    /**
     * Pontos decidem; no empate vale quem fez mais gols, depois quem deu mais
     * assistências e, por fim, a ordem alfabética para o resultado ser estável.
     */
    private Comparator<TotaisJogador> ordenacaoGeral() {
        Comparator<TotaisJogador> porPontos = Comparator.comparingInt(linha -> linha.resumo().pontuacao());
        Comparator<TotaisJogador> porGols = Comparator.comparingLong(TotaisJogador::gols);
        Comparator<TotaisJogador> porAssistencias = Comparator.comparingLong(TotaisJogador::assistencias);

        return porPontos.reversed()
                .thenComparing(porGols.reversed())
                .thenComparing(porAssistencias.reversed())
                .thenComparing(TotaisJogador::nickname, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * No ranking de um atributo, o empate é desfeito por quem fez mais com
     * menos jogos e depois pela pontuação geral.
     */
    private Comparator<TotaisJogador> ordenacaoPorAtributo(AtributoPontuacao atributo) {
        Comparator<TotaisJogador> porQuantidade =
                Comparator.comparingInt(linha -> atributo.quantidade(linha.resumo()));
        Comparator<TotaisJogador> porJogos = Comparator.comparingLong(TotaisJogador::jogos);
        Comparator<TotaisJogador> porPontos = Comparator.comparingInt(linha -> linha.resumo().pontuacao());

        return porQuantidade.reversed()
                .thenComparing(porJogos)
                .thenComparing(porPontos.reversed())
                .thenComparing(TotaisJogador::nickname, String.CASE_INSENSITIVE_ORDER);
    }
}
