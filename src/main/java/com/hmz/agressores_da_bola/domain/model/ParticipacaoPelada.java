package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import com.hmz.agressores_da_bola.domain.model.enums.StatusParticipacao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Entidade de ligação entre {@link Pelada} e {@link Usuario}. Não é um
 * ManyToMany simples porque a relação carrega dados próprios: o status da
 * presença e a data em que o jogador entrou na lista.
 */
@Entity
@Table(
        name = "tb_participacoes_pelada",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participacao_pelada_usuario",
                columnNames = {"pelada_id", "usuario_id"}
        )
)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipacaoPelada {

    /**
     * Ordem da escalação e da lista de espera: quem se inscreveu primeiro vem
     * primeiro.
     */
    public static final Comparator<ParticipacaoPelada> ORDEM_DE_INSCRICAO = Comparator.comparing(
            ParticipacaoPelada::getDataInscricao,
            Comparator.nullsLast(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pelada_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_participacao_pelada"))
    private Pelada pelada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_participacao_usuario"))
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusParticipacao status;

    @Column(name = "data_inscricao", nullable = false)
    private LocalDateTime dataInscricao;

    /**
     * Súmula do jogador nesta pelada. Segue o ciclo de vida da participação:
     * tirar o jogador da escalação apaga os números dele naquele jogo.
     */
    @OneToOne(mappedBy = "participacao", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private EstatisticaPartida estatistica;

    /**
     * Só a {@link Pelada} inscreve jogadores: é ela quem confere vaga e
     * duplicidade antes.
     */
    static ParticipacaoPelada inscrever(Usuario usuario, StatusParticipacao status, LocalDateTime agora) {
        ParticipacaoPelada participacao = new ParticipacaoPelada();
        participacao.usuario = usuario;
        participacao.status = status;
        participacao.dataInscricao = agora;
        return participacao;
    }

    void vincularA(Pelada pelada) {
        this.pelada = pelada;
    }

    void desvincular() {
        this.pelada = null;
    }

    void alterarStatus(StatusParticipacao novoStatus) {
        this.status = novoStatus;
    }

    /* ------------------------------------------------------------------
     * Súmula
     * ------------------------------------------------------------------ */

    /**
     * Lançar de novo corrige o lançamento anterior em vez de duplicar a
     * linha — a súmula de um jogador em uma pelada é única.
     *
     * @param posicaoInformada posição jogada; nula, vale a do cadastro
     */
    public EstatisticaPartida lancarEstatistica(Posicao posicaoInformada, ResumoEstatistico numeros) {
        pelada.garantirComJogo();
        garantirConfirmado();

        Posicao posicaoJogada = posicaoInformada != null ? posicaoInformada : usuario.getPosicao();
        EstatisticaPartida sumula = estatistica != null ? estatistica : EstatisticaPartida.da(this);
        sumula.registrar(posicaoJogada, numeros);

        this.estatistica = sumula;
        return sumula;
    }

    /**
     * Basta soltar a referência: o {@code orphanRemoval} apaga a súmula órfã
     * no flush, sem precisar mexer na chave estrangeira do outro lado.
     */
    public void removerEstatistica() {
        if (estatistica == null) {
            throw new RecursoNaoEncontradoException(
                    "O jogador de id " + usuario.getId()
                            + " não possui súmula lançada na pelada de id " + pelada.getId());
        }
        this.estatistica = null;
    }

    private void garantirConfirmado() {
        if (!estaConfirmado()) {
            throw new RegraDeNegocioException(
                    "O jogador '" + usuario.getNickname()
                            + "' não estava confirmado nesta pelada e não pode ter estatística");
        }
    }

    /* ------------------------------------------------------------------
     * Consultas
     * ------------------------------------------------------------------ */

    public boolean estaConfirmado() {
        return status != null && status.ocupaVaga();
    }

    public boolean estaNaListaDeEspera() {
        return status == StatusParticipacao.LISTA_DE_ESPERA;
    }

    public boolean pertenceAoUsuario(Long usuarioId) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(usuarioId);
    }

    @PrePersist
    private void aoPersistir() {
        if (dataInscricao == null) {
            dataInscricao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusParticipacao.CONVIDADO;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipacaoPelada outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
