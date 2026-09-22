package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.common.PageResponse;
import com.hmz.agressores_da_bola.application.pelada.dto.PeladaResponse;
import com.hmz.agressores_da_bola.application.pelada.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.application.port.UsuarioRepository;
import com.hmz.agressores_da_bola.domain.model.DadosPelada;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.model.Usuario;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Orquestra o agendamento: carrega, delega a regra à {@link Pelada}, grava e
 * devolve a resposta. As regras em si moram na entidade.
 */
@Service
@RequiredArgsConstructor
public class PeladaServiceImpl implements PeladaService {

    private static final String ALTERAR_PELADA = "alterar esta pelada";

    private final PeladaRepository peladaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeladaMapper peladaMapper;
    private final Clock relogio;

    @Override
    @Transactional
    public PeladaResponse criar(DadosPelada dados, Long usuarioLogadoId) {
        Usuario organizador = usuarioRepository.obter(usuarioLogadoId);
        Pelada pelada = Pelada.agendar(dados, organizador, agora(), peladaRepository);
        return peladaMapper.toResponse(peladaRepository.salvar(pelada));
    }

    @Override
    @Transactional(readOnly = true)
    public PeladaResponse buscarPorId(Long id) {
        return peladaMapper.toResponse(peladaRepository.obterComParticipantes(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PeladaResumoResponse> listar(PeladaFiltro filtro, Pageable pageable) {
        return PageResponse.de(peladaRepository.listar(filtro, pageable), peladaMapper::toResumoResponse);
    }

    @Override
    @Transactional
    public PeladaResponse atualizar(Long id, DadosPelada dados, Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obterComParticipantes(id);
        pelada.exigirOrganizador(usuarioLogadoId, ALTERAR_PELADA);
        pelada.atualizar(dados, agora(), peladaRepository);
        return peladaMapper.toResponse(peladaRepository.salvar(pelada));
    }

    @Override
    @Transactional
    public PeladaResponse alterarStatus(Long id, StatusPelada novoStatus, Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obterComParticipantes(id);
        pelada.exigirOrganizador(usuarioLogadoId, ALTERAR_PELADA);
        pelada.alterarStatus(novoStatus);
        return peladaMapper.toResponse(peladaRepository.salvar(pelada));
    }

    @Override
    @Transactional
    public void deletar(Long id, Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obter(id);
        pelada.exigirOrganizador(usuarioLogadoId, ALTERAR_PELADA);
        peladaRepository.remover(pelada);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
