package com.hmz.agressores_da_bola.application.estatistica;

import com.hmz.agressores_da_bola.application.estatistica.dto.EstatisticaResponse;
import com.hmz.agressores_da_bola.application.port.EstatisticaRepository;
import com.hmz.agressores_da_bola.application.port.ParticipacaoRepository;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Orquestra a súmula. Quando se pode lançar, para quem e quais números valem
 * em cada posição são regras da {@link ParticipacaoPelada} e da
 * {@link EstatisticaPartida}.
 */
@Service
@RequiredArgsConstructor
public class EstatisticaServiceImpl implements EstatisticaService {

    private static final String LANCAR_SUMULA = "lançar ou apagar a súmula desta pelada";

    private static final Comparator<EstatisticaPartida> MAIOR_PONTUACAO_PRIMEIRO =
            Comparator.comparingInt(EstatisticaPartida::pontuacao).reversed()
                    .thenComparing(estatistica -> estatistica.getJogador().getNickname());

    private final EstatisticaRepository estatisticaRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final PeladaRepository peladaRepository;
    private final EstatisticaMapper estatisticaMapper;

    @Override
    @Transactional
    public EstatisticaResponse registrar(Long peladaId, Long usuarioId, LancamentoEstatistica lancamento,
                                         Long usuarioLogadoId) {
        ParticipacaoPelada participacao = participacaoRepository.obter(peladaId, usuarioId);
        participacao.getPelada().exigirOrganizador(usuarioLogadoId, LANCAR_SUMULA);

        EstatisticaPartida estatistica =
                participacao.lancarEstatistica(lancamento.posicaoJogada(), lancamento.numeros());

        return estatisticaMapper.toResponse(estatisticaRepository.salvar(estatistica));
    }

    @Override
    @Transactional(readOnly = true)
    public EstatisticaResponse buscar(Long peladaId, Long usuarioId) {
        ParticipacaoPelada participacao = participacaoRepository.obter(peladaId, usuarioId);

        EstatisticaPartida estatistica = estatisticaRepository
                .buscarDaParticipacao(participacao.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O jogador de id " + usuarioId
                                + " ainda não teve a súmula lançada na pelada de id " + peladaId));

        return estatisticaMapper.toResponse(estatistica);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstatisticaResponse> listarDaPelada(Long peladaId) {
        peladaRepository.garantirQueExiste(peladaId);

        return estatisticaRepository.listarDaPelada(peladaId).stream()
                .sorted(MAIOR_PONTUACAO_PRIMEIRO)
                .map(estatisticaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void remover(Long peladaId, Long usuarioId, Long usuarioLogadoId) {
        ParticipacaoPelada participacao = participacaoRepository.obter(peladaId, usuarioId);
        participacao.getPelada().exigirOrganizador(usuarioLogadoId, LANCAR_SUMULA);

        participacao.removerEstatistica();
        participacaoRepository.salvar(participacao);
    }
}
