package com.hmz.agressores_da_bola.model;

import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "tb_peladas")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pelada {

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
    private List<ParticipacaoPelada> participacoes = new ArrayList<>();

    /* ------------------------------------------------------------------
     * Comportamento de domínio: a entidade protege as próprias invariantes
     * em vez de deixar o service manipular a coleção por fora.
     * ------------------------------------------------------------------ */

    public void adicionarParticipacao(ParticipacaoPelada participacao) {
        participacao.setPelada(this);
        participacoes.add(participacao);
    }

    public void removerParticipacao(ParticipacaoPelada participacao) {
        participacoes.remove(participacao);
        participacao.setPelada(null);
    }

    public Optional<ParticipacaoPelada> buscarParticipacaoDoUsuario(Long usuarioId) {
        return participacoes.stream()
                .filter(participacao -> participacao.pertenceAoUsuario(usuarioId))
                .findFirst();
    }

    public int totalConfirmados() {
        return (int) participacoes.stream()
                .filter(participacao -> participacao.getStatus().ocupaVaga())
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

    public boolean aceitaAlteracoes() {
        return status != null && !status.encerrada();
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
