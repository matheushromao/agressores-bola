package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.pelada.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.application.port.ParticipacaoRepository;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.domain.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquestra a escalação. Vagas, lista de espera e quem pode sair são regras
 * da {@link Pelada}; aqui só se carrega, delega e grava.
 */
@Service
@RequiredArgsConstructor
public class EscalacaoServiceImpl implements EscalacaoService {

    private final PeladaRepository peladaRepository;
    private final ParticipacaoRepository participacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeladaMapper peladaMapper;
    private final Clock relogio;

    @Override
    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listar(Long peladaId) {
        peladaRepository.garantirQueExiste(peladaId);
        return participacaoRepository.listarDaPelada(peladaId).stream()
                .map(peladaMapper::toParticipanteResponse)
                .toList();
    }

    @Override
    @Transactional
    public ParticipanteResponse adicionar(Long peladaId, Long usuarioId, StatusParticipacao status,
                                          Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obterComParticipantes(peladaId);
        pelada.exigirPermissaoParaEscalar(usuarioId, usuarioLogadoId);
        pelada.garantirAberta();

        Usuario jogador = usuarioRepository.obter(usuarioId);
        ParticipacaoPelada participacao = pelada.escalar(jogador, status, LocalDateTime.now(relogio));

        // Persistida pelo próprio repositório (e não só via cascade da pelada)
        // para que o id gerado já esteja disponível na resposta.
        return peladaMapper.toParticipanteResponse(participacaoRepository.salvar(participacao));
    }

    @Override
    @Transactional
    public ParticipanteResponse alterarStatus(Long peladaId, Long usuarioId, StatusParticipacao novoStatus,
                                              Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obterComParticipantes(peladaId);
        pelada.exigirOrganizadorOuProprioJogador(usuarioId, usuarioLogadoId);

        ParticipacaoPelada participacao = pelada.alterarParticipacao(usuarioId, novoStatus);

        peladaRepository.salvar(pelada);
        return peladaMapper.toParticipanteResponse(participacao);
    }

    @Override
    @Transactional
    public void remover(Long peladaId, Long usuarioId, Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obterComParticipantes(peladaId);
        pelada.exigirOrganizadorOuProprioJogador(usuarioId, usuarioLogadoId);

        pelada.removerParticipante(usuarioId);

        peladaRepository.salvar(pelada);
    }
}
