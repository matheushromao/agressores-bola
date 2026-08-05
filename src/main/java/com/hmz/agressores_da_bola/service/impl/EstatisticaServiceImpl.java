package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.EstatisticaRequest;
import com.hmz.agressores_da_bola.dto.EstatisticaResponse;
import com.hmz.agressores_da_bola.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.EstatisticaMapper;
import com.hmz.agressores_da_bola.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.ResumoEstatistico;
import com.hmz.agressores_da_bola.model.enums.AtributoPontuacao;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import com.hmz.agressores_da_bola.repository.EstatisticaPartidaRepository;
import com.hmz.agressores_da_bola.repository.ParticipacaoPeladaRepository;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import com.hmz.agressores_da_bola.service.EstatisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstatisticaServiceImpl implements EstatisticaService {

    private final EstatisticaPartidaRepository estatisticaRepository;
    private final ParticipacaoPeladaRepository participacaoRepository;
    private final PeladaRepository peladaRepository;
    private final EstatisticaMapper estatisticaMapper;

    @Override
    @Transactional
    public EstatisticaResponse registrar(Long peladaId, Long usuarioId, EstatisticaRequest request) {
        ParticipacaoPelada participacao = obterParticipacao(peladaId, usuarioId);

        validarPeladaComJogo(participacao.getPelada());
        validarJogadorConfirmado(participacao);

        Posicao posicaoJogada = posicaoJogada(participacao, request);
        validarAtributosDaPosicao(posicaoJogada, request.paraResumo());

        // Lançar de novo corrige o lançamento anterior em vez de duplicar a
        // linha — a súmula de um jogador em uma pelada é única.
        EstatisticaPartida estatistica = estatisticaRepository
                .findByParticipacaoId(participacao.getId())
                .orElseGet(() -> novaEstatistica(participacao));

        aplicar(request, posicaoJogada, estatistica);

        return estatisticaMapper.toResponse(estatisticaRepository.save(estatistica));
    }

    @Override
    @Transactional(readOnly = true)
    public EstatisticaResponse buscar(Long peladaId, Long usuarioId) {
        ParticipacaoPelada participacao = obterParticipacao(peladaId, usuarioId);

        EstatisticaPartida estatistica = estatisticaRepository
                .findByParticipacaoId(participacao.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O jogador de id " + usuarioId
                                + " ainda não teve a súmula lançada na pelada de id " + peladaId));

        return estatisticaMapper.toResponse(estatistica);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstatisticaResponse> listarDaPelada(Long peladaId) {
        garantirQuePeladaExiste(peladaId);

        Comparator<EstatisticaPartida> porPontuacao =
                Comparator.comparingInt(EstatisticaPartida::pontuacao);
        Comparator<EstatisticaPartida> porNickname =
                Comparator.comparing(estatistica -> estatistica.getJogador().getNickname());

        return estatisticaRepository.buscarDaPelada(peladaId).stream()
                .sorted(porPontuacao.reversed().thenComparing(porNickname))
                .map(estatisticaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void remover(Long peladaId, Long usuarioId) {
        ParticipacaoPelada participacao = obterParticipacao(peladaId, usuarioId);

        if (estatisticaRepository.findByParticipacaoId(participacao.getId()).isEmpty()) {
            throw new RecursoNaoEncontradoException(
                    "O jogador de id " + usuarioId
                            + " não possui súmula lançada na pelada de id " + peladaId);
        }

        participacao.removerEstatistica();
        participacaoRepository.save(participacao);
    }

    /* ------------------------------------------------------------------
     * Consultas internas
     * ------------------------------------------------------------------ */

    private ParticipacaoPelada obterParticipacao(Long peladaId, Long usuarioId) {
        return participacaoRepository.findByPeladaIdAndUsuarioId(peladaId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O usuário de id " + usuarioId + " não participa da pelada de id " + peladaId));
    }

    private void garantirQuePeladaExiste(Long peladaId) {
        if (!peladaRepository.existsById(peladaId)) {
            throw new RecursoNaoEncontradoException("Pelada não encontrada com o id: " + peladaId);
        }
    }

    private EstatisticaPartida novaEstatistica(ParticipacaoPelada participacao) {
        EstatisticaPartida estatistica = EstatisticaPartida.builder().build();
        participacao.definirEstatistica(estatistica);
        return estatistica;
    }

    private void aplicar(EstatisticaRequest request, Posicao posicaoJogada, EstatisticaPartida estatistica) {
        ResumoEstatistico resumo = request.paraResumo();

        estatistica.setPosicaoJogada(posicaoJogada);
        estatistica.setGols(resumo.gols());
        estatistica.setAssistencias(resumo.assistencias());
        estatistica.setDesarmes(resumo.desarmes());
        estatistica.setDefesas(resumo.defesas());
        estatistica.setDefesasDificeis(resumo.defesasDificeis());
    }

    /* ------------------------------------------------------------------
     * Regras de negócio
     * ------------------------------------------------------------------ */

    private Posicao posicaoJogada(ParticipacaoPelada participacao, EstatisticaRequest request) {
        return request.posicaoJogada() != null
                ? request.posicaoJogada()
                : participacao.getUsuario().getPosicao();
    }

    private void validarPeladaComJogo(Pelada pelada) {
        if (!pelada.getStatus().aceitaEstatistica()) {
            throw new RegraDeNegocioException(
                    "A pelada está " + pelada.getStatus().getDescricao().toLowerCase()
                            + " e ainda não tem súmula. Só é possível lançar estatística de "
                            + "pelada em andamento ou finalizada");
        }
    }

    private void validarJogadorConfirmado(ParticipacaoPelada participacao) {
        if (!participacao.estaConfirmado()) {
            throw new RegraDeNegocioException(
                    "O jogador '" + participacao.getUsuario().getNickname()
                            + "' não estava confirmado nesta pelada e não pode ter estatística");
        }
    }

    /**
     * Cada posição tem a sua ficha: quem jogou no gol soma defesas, quem jogou
     * na linha soma desarmes. Misturar as duas mascararia o desempenho e
     * distorceria os rankings por atributo.
     */
    private void validarAtributosDaPosicao(Posicao posicaoJogada, ResumoEstatistico resumo) {
        boolean goleiro = posicaoJogada == Posicao.GOLEIRO;

        for (AtributoPontuacao atributo : AtributoPontuacao.values()) {
            if (atributo.quantidade(resumo) == 0) {
                continue;
            }
            if (!goleiro && atributo.exclusivoDeGoleiro()) {
                throw new RegraDeNegocioException(
                        "O atributo '" + atributo.getDescricao() + "' só vale para quem jogou no gol. "
                                + "Informe posicaoJogada=GOLEIRO se ele pegou o gol na pelada");
            }
            if (goleiro && atributo.exclusivoDeLinha()) {
                throw new RegraDeNegocioException(
                        "O atributo '" + atributo.getDescricao() + "' não é contabilizado para goleiros");
            }
        }
    }
}
