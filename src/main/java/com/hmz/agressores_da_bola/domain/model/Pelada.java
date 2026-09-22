package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.domain.model.enums.TipoCampo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A pelada é a raiz do agregado da escalação: toda regra sobre quem entra,
 * quem sai, quem espera e quem pode mexer mora aqui, e não nos services.
 * Não há setters — o estado só muda pelos métodos que protegem as invariantes.
 */
@Entity
@Table(name = "tb_peladas")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pelada {

    /**
     * Com menos que isso não há jogo: confirmar a pelada exige ao menos dois.
     */
    private static final int MINIMO_DE_CONFIRMADOS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "local_nome", nullable = false, length = 120)
    private String localNome;

    @Column(nullable = false, length = 200)
    private String endereco;

    @Column(nullable = false, length = 80)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_campo", nullable = false, length = 20)
    private TipoCampo tipoCampo;

    @Column(name = "max_participantes", nullable = false)
    private Integer maxParticipantes;

    @Column(name = "valor_por_jogador", precision = 10, scale = 2)
    private BigDecimal valorPorJogador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPelada status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizador_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pelada_organizador"))
    private Usuario organizador;

    /**
     * O ciclo de vida das participações pertence à pelada: remover a pelada
     * remove a escalação, e a escalação não existe fora dela.
     * O @BatchSize evita o N+1 ao listar várias peladas paginadas.
     */
    @OneToMany(mappedBy = "pelada", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private List<ParticipacaoPelada> participacoes = new ArrayList<>();

    /* ------------------------------------------------------------------
     * Agendamento
     * ------------------------------------------------------------------ */

    /**
     * Quem organiza já entra escalado e confirmado: é o comportamento
     * esperado pelo usuário e garante que a pelada nunca nasce vazia.
     */
    public static Pelada agendar(DadosPelada dados, Usuario organizador,
                                 LocalDateTime agora, AgendaDePeladas agenda) {
        garantirInicioNoFuturo(dados, agora);
        garantirAgendaLivre(agenda, organizador.getId(), dados);

        Pelada pelada = new Pelada();
        pelada.organizador = organizador;
        pelada.status = StatusPelada.AGENDADA;
        pelada.aplicar(dados);
        pelada.vincular(ParticipacaoPelada.inscrever(organizador, StatusParticipacao.CONFIRMADO, agora));
        return pelada;
    }

    public void atualizar(DadosPelada dados, LocalDateTime agora, AgendaDePeladas agenda) {
        garantirAberta();
        garantirInicioNoFuturo(dados, agora);
        garantirLimiteAcimaDosConfirmados(dados.maxParticipantes());

        if (mudariaAgenda(dados)) {
            garantirAgendaLivre(agenda, organizador.getId(), dados);
        }
        aplicar(dados);
    }

    public void alterarStatus(StatusPelada novoStatus) {
        if (status == novoStatus) {
            return;
        }
        if (status.encerrada()) {
            throw new RegraDeNegocioException(situacao() + " e o status não pode mais ser alterado");
        }
        if (novoStatus == StatusPelada.CONFIRMADA && totalConfirmados() < MINIMO_DE_CONFIRMADOS) {
            throw new RegraDeNegocioException(
                    "A pelada precisa de pelo menos " + MINIMO_DE_CONFIRMADOS
                            + " jogadores confirmados para ser confirmada");
        }
        status = novoStatus;
    }

    /* ------------------------------------------------------------------
     * Escalação
     * ------------------------------------------------------------------ */

    public ParticipacaoPelada escalar(Usuario jogador, StatusParticipacao status, LocalDateTime agora) {
        garantirAberta();
        if (buscarParticipacaoDoUsuario(jogador.getId()).isPresent()) {
            throw new RegraDeNegocioException(
                    "O jogador '" + jogador.getNickname() + "' já faz parte desta pelada");
        }
        garantirVagaPara(status);

        ParticipacaoPelada participacao = ParticipacaoPelada.inscrever(jogador, status, agora);
        vincular(participacao);
        return participacao;
    }

    public ParticipacaoPelada alterarParticipacao(Long usuarioId, StatusParticipacao novoStatus) {
        garantirAberta();
        ParticipacaoPelada participacao = obterParticipacao(usuarioId);

        if (participacao.getStatus() == novoStatus) {
            return participacao;
        }
        if (organizadaPor(usuarioId) && !novoStatus.ocupaVaga()) {
            throw new RegraDeNegocioException(
                    "O organizador não pode sair da própria pelada. Cancele a pelada ou transfira a organização");
        }
        garantirVagaPara(novoStatus);
        participacao.alterarStatus(novoStatus);

        if (!novoStatus.ocupaVaga()) {
            promoverPrimeiroDaListaDeEspera();
        }
        return participacao;
    }

    public void removerParticipante(Long usuarioId) {
        garantirAberta();
        if (organizadaPor(usuarioId)) {
            throw new RegraDeNegocioException("O organizador não pode ser removido da própria pelada");
        }

        ParticipacaoPelada participacao = obterParticipacao(usuarioId);
        boolean liberouVaga = participacao.estaConfirmado();

        participacoes.remove(participacao);
        participacao.desvincular();

        if (liberouVaga) {
            promoverPrimeiroDaListaDeEspera();
        }
    }

    /**
     * Sempre que uma vaga é liberada, o primeiro jogador que entrou na lista
     * de espera é promovido a confirmado.
     */
    private void promoverPrimeiroDaListaDeEspera() {
        if (estaLotada()) {
            return;
        }
        participacoes.stream()
                .filter(ParticipacaoPelada::estaNaListaDeEspera)
                .min(ParticipacaoPelada.ORDEM_DE_INSCRICAO)
                .ifPresent(participacao -> participacao.alterarStatus(StatusParticipacao.CONFIRMADO));
    }

    /* ------------------------------------------------------------------
     * Posse: só quem organiza altera dados, status, súmula e sorteio
     * ------------------------------------------------------------------ */

    public boolean organizadaPor(Long usuarioId) {
        return organizador != null
                && organizador.getId() != null
                && organizador.getId().equals(usuarioId);
    }

    /**
     * @param acao o que foi tentado, completando "Só o organizador pode ..."
     */
    public void exigirOrganizador(Long usuarioLogadoId, String acao) {
        if (!organizadaPor(usuarioLogadoId)) {
            throw new AcessoNegadoException("Só o organizador pode " + acao);
        }
    }

    /**
     * O jogador entra sozinho; incluir outra pessoa é prerrogativa de quem organiza.
     */
    public void exigirPermissaoParaEscalar(Long usuarioId, Long usuarioLogadoId) {
        if (!organizadaPor(usuarioLogadoId) && !usuarioId.equals(usuarioLogadoId)) {
            throw new AcessoNegadoException(
                    "Só o organizador pode incluir outros jogadores. Para entrar, informe o seu próprio usuarioId");
        }
    }

    public void exigirOrganizadorOuProprioJogador(Long usuarioId, Long usuarioLogadoId) {
        if (!organizadaPor(usuarioLogadoId) && !usuarioId.equals(usuarioLogadoId)) {
            throw new AcessoNegadoException(
                    "Só o organizador ou o próprio jogador podem alterar esta participação");
        }
    }

    /* ------------------------------------------------------------------
     * Situação da pelada
     * ------------------------------------------------------------------ */

    public void garantirAberta() {
        if (!aceitaAlteracoes()) {
            throw new RegraDeNegocioException(situacao() + " e não aceita mais alterações");
        }
    }

    public void garantirSorteavel() {
        if (!status.aceitaSorteio()) {
            throw new RegraDeNegocioException(situacao() + " e não faz mais sentido sortear times");
        }
    }

    public void garantirComJogo() {
        if (!status.aceitaEstatistica()) {
            throw new RegraDeNegocioException(situacao()
                    + " e ainda não tem súmula. Só é possível lançar estatística de "
                    + "pelada em andamento ou finalizada");
        }
    }

    public boolean aceitaAlteracoes() {
        return status != null && !status.encerrada();
    }

    /* ------------------------------------------------------------------
     * Consultas
     * ------------------------------------------------------------------ */

    /**
     * A escalação não é alterada por fora: quem precisa mudá-la usa os
     * métodos de escalação, que protegem vagas e lista de espera.
     */
    public List<ParticipacaoPelada> getParticipacoes() {
        return Collections.unmodifiableList(participacoes);
    }

    public List<ParticipacaoPelada> escalacaoPorOrdemDeInscricao() {
        return participacoes.stream()
                .sorted(ParticipacaoPelada.ORDEM_DE_INSCRICAO)
                .toList();
    }

    /**
     * Só quem confirmou presença joga: convidado e lista de espera ainda não
     * são jogadores da pelada.
     */
    public List<Usuario> jogadoresConfirmados() {
        return participacoes.stream()
                .filter(ParticipacaoPelada::estaConfirmado)
                .map(ParticipacaoPelada::getUsuario)
                .toList();
    }

    public Optional<ParticipacaoPelada> buscarParticipacaoDoUsuario(Long usuarioId) {
        return participacoes.stream()
                .filter(participacao -> participacao.pertenceAoUsuario(usuarioId))
                .findFirst();
    }

    public int totalConfirmados() {
        return (int) participacoes.stream()
                .filter(ParticipacaoPelada::estaConfirmado)
                .count();
    }

    public int vagasRestantes() {
        return Math.max(0, maxParticipantes - totalConfirmados());
    }

    public boolean estaLotada() {
        return vagasRestantes() == 0;
    }

    public LocalDateTime inicio() {
        return LocalDateTime.of(data, horaInicio);
    }

    /* ------------------------------------------------------------------
     * Internos
     * ------------------------------------------------------------------ */

    private void aplicar(DadosPelada dados) {
        this.nome = dados.nome();
        this.descricao = dados.descricao();
        this.data = dados.data();
        this.horaInicio = dados.horaInicio();
        this.horaFim = dados.horaFim();
        this.localNome = dados.localNome();
        this.endereco = dados.endereco();
        this.cidade = dados.cidade();
        this.estado = dados.estado().toUpperCase();
        this.tipoCampo = dados.tipoCampo();
        this.maxParticipantes = dados.maxParticipantes();
        this.valorPorJogador = dados.valorPorJogador();
    }

    private void vincular(ParticipacaoPelada participacao) {
        participacao.vincularA(this);
        participacoes.add(participacao);
    }

    private ParticipacaoPelada obterParticipacao(Long usuarioId) {
        return buscarParticipacaoDoUsuario(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.participacao(id, usuarioId));
    }

    private boolean mudariaAgenda(DadosPelada dados) {
        return !data.equals(dados.data()) || !horaInicio.equals(dados.horaInicio());
    }

    private void garantirLimiteAcimaDosConfirmados(int novoLimite) {
        int confirmados = totalConfirmados();
        if (novoLimite < confirmados) {
            throw new RegraDeNegocioException(
                    "A pelada já possui " + confirmados + " jogadores confirmados. "
                            + "O limite não pode ser menor que isso");
        }
    }

    private void garantirVagaPara(StatusParticipacao status) {
        if (status.ocupaVaga() && estaLotada()) {
            throw new RegraDeNegocioException(
                    "A pelada já atingiu o limite de " + maxParticipantes
                            + " jogadores confirmados. Entre na lista de espera");
        }
    }

    private String situacao() {
        return "A pelada está " + status.getDescricao().toLowerCase();
    }

    private static void garantirInicioNoFuturo(DadosPelada dados, LocalDateTime agora) {
        if (dados.inicio().isBefore(agora)) {
            throw new RegraDeNegocioException(
                    "O horário de início já passou. Escolha uma data e hora futuras");
        }
    }

    private static void garantirAgendaLivre(AgendaDePeladas agenda, Long organizadorId, DadosPelada dados) {
        if (agenda.organizadorTemPeladaEm(organizadorId, dados.data(), dados.horaInicio())) {
            throw new RegraDeNegocioException(
                    "O organizador já possui uma pelada marcada para esta data e horário");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pelada outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
