package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.PageResponse;
import com.hmz.agressores_da_bola.dto.ParticipacaoRequest;
import com.hmz.agressores_da_bola.dto.ParticipanteResponse;
import com.hmz.agressores_da_bola.dto.PeladaFiltro;
import com.hmz.agressores_da_bola.dto.PeladaRequest;
import com.hmz.agressores_da_bola.dto.PeladaResponse;
import com.hmz.agressores_da_bola.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.PeladaMapper;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.repository.ParticipacaoPeladaRepository;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import com.hmz.agressores_da_bola.repository.UsuarioRepository;
import com.hmz.agressores_da_bola.repository.specification.PeladaSpecification;
import com.hmz.agressores_da_bola.service.PeladaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeladaServiceImpl implements PeladaService {

    private final PeladaRepository peladaRepository;
    private final ParticipacaoPeladaRepository participacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeladaMapper peladaMapper;

    /* ------------------------------------------------------------------
     * Pelada
     * ------------------------------------------------------------------ */

    @Override
    @Transactional
    public PeladaResponse criar(PeladaRequest request) {
        Usuario organizador = obterUsuario(request.organizadorId());

        validarInicioNoFuturo(request);
        validarAgendaDoOrganizador(organizador.getId(), request);

        Pelada pelada = peladaMapper.toEntity(request, organizador);

        // Quem organiza já entra escalado e confirmado: é o comportamento
        // esperado pelo usuário e garante que a pelada nunca nasce vazia.
        pelada.adicionarParticipacao(ParticipacaoPelada.builder()
                .usuario(organizador)
                .status(StatusParticipacao.CONFIRMADO)
                .dataInscricao(LocalDateTime.now())
                .build());

        return peladaMapper.toResponse(peladaRepository.save(pelada));
    }

    @Override
    @Transactional(readOnly = true)
    public PeladaResponse buscarPorId(Long id) {
        return peladaMapper.toResponse(obterPeladaComParticipantes(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PeladaResumoResponse> listar(PeladaFiltro filtro, Pageable pageable) {
        Specification<Pelada> filtros = Specification.allOf(
                PeladaSpecification.comStatus(filtro.status()),
                PeladaSpecification.comTipoCampo(filtro.tipoCampo()),
                PeladaSpecification.naCidade(filtro.cidade()),
                PeladaSpecification.aPartirDe(filtro.dataInicial()),
                PeladaSpecification.ate(filtro.dataFinal()),
                PeladaSpecification.doOrganizador(filtro.organizadorId()),
                PeladaSpecification.comParticipante(filtro.participanteId())
        );

        Page<Pelada> pagina = peladaRepository.findAll(filtros, pageable);
        return PageResponse.de(pagina, peladaMapper::toResumoResponse);
    }

    @Override
    @Transactional
    public PeladaResponse atualizar(Long id, PeladaRequest request) {
        Pelada pelada = obterPeladaComParticipantes(id);
        validarPeladaAberta(pelada);

        validarInicioNoFuturo(request);
        validarTrocaDeOrganizadorNaoSolicitada(pelada, request);
        validarLimiteAcimaDosConfirmados(pelada, request.maxParticipantes());

        if (mudouAgenda(pelada, request)) {
            validarAgendaDoOrganizador(pelada.getOrganizador().getId(), request);
        }

        peladaMapper.copyToEntity(request, pelada);
        return peladaMapper.toResponse(peladaRepository.save(pelada));
    }

    @Override
    @Transactional
    public PeladaResponse alterarStatus(Long id, StatusPelada novoStatus) {
        Pelada pelada = obterPeladaComParticipantes(id);
        validarTransicaoDeStatus(pelada, novoStatus);

        pelada.setStatus(novoStatus);
        return peladaMapper.toResponse(peladaRepository.save(pelada));
    }

    @Override
    @Transactional
    public void deletar(Long id) {
        peladaRepository.delete(obterPelada(id));
    }

    /* ------------------------------------------------------------------
     * Escalação
     * ------------------------------------------------------------------ */

    @Override
    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarParticipantes(Long peladaId) {
        garantirQuePeladaExiste(peladaId);
        return participacaoRepository.findByPeladaIdOrderByDataInscricaoAsc(peladaId)
                .stream()
                .map(peladaMapper::toParticipanteResponse)
                .toList();
    }

    @Override
    @Transactional
    public ParticipanteResponse adicionarParticipante(Long peladaId, ParticipacaoRequest request) {
        Pelada pelada = obterPeladaComParticipantes(peladaId);
        validarPeladaAberta(pelada);

        Usuario usuario = obterUsuario(request.usuarioId());

        if (pelada.buscarParticipacaoDoUsuario(usuario.getId()).isPresent()) {
            throw new RegraDeNegocioException(
                    "O jogador '" + usuario.getNickname() + "' já faz parte desta pelada");
        }

        StatusParticipacao status = request.statusOuPadrao();
        validarVagaDisponivel(pelada, status);

        ParticipacaoPelada participacao = ParticipacaoPelada.builder()
                .usuario(usuario)
                .status(status)
                .dataInscricao(LocalDateTime.now())
                .build();

        pelada.adicionarParticipacao(participacao);

        // Persistido direto pelo próprio repositório (e não só via cascade da
        // pelada) para que o id gerado já esteja disponível na resposta.
        participacaoRepository.save(participacao);

        return peladaMapper.toParticipanteResponse(participacao);
    }

    @Override
    @Transactional
    public ParticipanteResponse alterarStatusParticipacao(Long peladaId, Long usuarioId, StatusParticipacao novoStatus) {
        Pelada pelada = obterPeladaComParticipantes(peladaId);
        validarPeladaAberta(pelada);

        ParticipacaoPelada participacao = obterParticipacao(pelada, usuarioId);

        if (participacao.getStatus() == novoStatus) {
            return peladaMapper.toParticipanteResponse(participacao);
        }

        if (ehOrganizador(pelada, usuarioId) && !novoStatus.ocupaVaga()) {
            throw new RegraDeNegocioException(
                    "O organizador não pode sair da própria pelada. Cancele a pelada ou transfira a organização");
        }

        validarVagaDisponivel(pelada, novoStatus);
        participacao.setStatus(novoStatus);

        // Quem desocupa uma vaga libera lugar para o primeiro da fila.
        if (!novoStatus.ocupaVaga()) {
            promoverPrimeiroDaListaDeEspera(pelada);
        }

        peladaRepository.save(pelada);
        return peladaMapper.toParticipanteResponse(participacao);
    }

    @Override
    @Transactional
    public void removerParticipante(Long peladaId, Long usuarioId) {
        Pelada pelada = obterPeladaComParticipantes(peladaId);
        validarPeladaAberta(pelada);

        if (ehOrganizador(pelada, usuarioId)) {
            throw new RegraDeNegocioException(
                    "O organizador não pode ser removido da própria pelada");
        }

        ParticipacaoPelada participacao = obterParticipacao(pelada, usuarioId);
        boolean liberouVaga = participacao.getStatus().ocupaVaga();

        pelada.removerParticipacao(participacao);

        if (liberouVaga) {
            promoverPrimeiroDaListaDeEspera(pelada);
        }

        peladaRepository.save(pelada);
    }

    /* ------------------------------------------------------------------
     * Consultas internas
     * ------------------------------------------------------------------ */

    private Pelada obterPelada(Long id) {
        return peladaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pelada não encontrada com o id: " + id));
    }

    private Pelada obterPeladaComParticipantes(Long id) {
        return peladaRepository.buscarComParticipantes(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pelada não encontrada com o id: " + id));
    }

    private void garantirQuePeladaExiste(Long id) {
        if (!peladaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Pelada não encontrada com o id: " + id);
        }
    }

    private Usuario obterUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com o id: " + id));
    }

    private ParticipacaoPelada obterParticipacao(Pelada pelada, Long usuarioId) {
        return pelada.buscarParticipacaoDoUsuario(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O usuário de id " + usuarioId + " não participa da pelada de id " + pelada.getId()));
    }

    private boolean ehOrganizador(Pelada pelada, Long usuarioId) {
        return pelada.getOrganizador() != null
                && pelada.getOrganizador().getId().equals(usuarioId);
    }

    /* ------------------------------------------------------------------
     * Regras de negócio
     * ------------------------------------------------------------------ */

    private void validarInicioNoFuturo(PeladaRequest request) {
        LocalDateTime inicio = LocalDateTime.of(request.data(), request.horaInicio());
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException(
                    "O horário de início já passou. Escolha uma data e hora futuras");
        }
    }

    private void validarAgendaDoOrganizador(Long organizadorId, PeladaRequest request) {
        if (peladaRepository.existsByOrganizadorIdAndDataAndHoraInicio(
                organizadorId, request.data(), request.horaInicio())) {
            throw new RegraDeNegocioException(
                    "O organizador já possui uma pelada marcada para esta data e horário");
        }
    }

    private boolean mudouAgenda(Pelada pelada, PeladaRequest request) {
        return !pelada.getData().equals(request.data())
                || !pelada.getHoraInicio().equals(request.horaInicio());
    }

    private void validarTrocaDeOrganizadorNaoSolicitada(Pelada pelada, PeladaRequest request) {
        if (!pelada.getOrganizador().getId().equals(request.organizadorId())) {
            throw new RegraDeNegocioException(
                    "A troca de organizador não é permitida na atualização da pelada");
        }
    }

    private void validarLimiteAcimaDosConfirmados(Pelada pelada, Integer novoLimite) {
        int confirmados = pelada.totalConfirmados();
        if (novoLimite < confirmados) {
            throw new RegraDeNegocioException(
                    "A pelada já possui " + confirmados + " jogadores confirmados. "
                            + "O limite não pode ser menor que isso");
        }
    }

    private void validarPeladaAberta(Pelada pelada) {
        if (!pelada.aceitaAlteracoes()) {
            throw new RegraDeNegocioException(
                    "A pelada está " + pelada.getStatus().getDescricao().toLowerCase()
                            + " e não aceita mais alterações");
        }
    }

    private void validarVagaDisponivel(Pelada pelada, StatusParticipacao status) {
        if (status.ocupaVaga() && pelada.estaLotada()) {
            throw new RegraDeNegocioException(
                    "A pelada já atingiu o limite de " + pelada.getMaxParticipantes()
                            + " jogadores confirmados. Entre na lista de espera");
        }
    }

    private void validarTransicaoDeStatus(Pelada pelada, StatusPelada novoStatus) {
        if (pelada.getStatus() == novoStatus) {
            return;
        }
        if (pelada.getStatus().encerrada()) {
            throw new RegraDeNegocioException(
                    "A pelada está " + pelada.getStatus().getDescricao().toLowerCase()
                            + " e o status não pode mais ser alterado");
        }
        if (novoStatus == StatusPelada.CONFIRMADA && pelada.totalConfirmados() < 2) {
            throw new RegraDeNegocioException(
                    "A pelada precisa de pelo menos 2 jogadores confirmados para ser confirmada");
        }
    }

    /**
     * Sempre que uma vaga é liberada, o primeiro jogador que entrou na lista
     * de espera é promovido a confirmado.
     */
    private void promoverPrimeiroDaListaDeEspera(Pelada pelada) {
        if (pelada.estaLotada()) {
            return;
        }
        pelada.getParticipacoes().stream()
                .filter(participacao -> participacao.getStatus() == StatusParticipacao.LISTA_DE_ESPERA)
                .min(Comparator.comparing(
                        ParticipacaoPelada::getDataInscricao,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .ifPresent(participacao -> participacao.setStatus(StatusParticipacao.CONFIRMADO));
    }
}
