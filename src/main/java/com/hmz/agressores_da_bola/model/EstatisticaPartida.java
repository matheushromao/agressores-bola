package com.hmz.agressores_da_bola.model;

import com.hmz.agressores_da_bola.model.enums.Posicao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A súmula de um jogador em uma pelada. Fica pendurada na
 * {@link ParticipacaoPelada} (e não no {@link Usuario}) porque a estatística
 * é do jogo: o mesmo jogador tem uma linha diferente em cada pelada.
 *
 * <p>A pontuação não é persistida de propósito — ela é derivada dos números
 * e dos pesos de {@code AtributoPontuacao}. Guardá-la deixaria o histórico
 * inconsistente no dia em que a tabela de pontos mudar.</p>
 */
@Entity
@Table(
        name = "tb_estatisticas_partida",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estatistica_participacao",
                columnNames = "participacao_id"
        )
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EstatisticaPartida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participacao_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_estatistica_participacao"))
    private ParticipacaoPelada participacao;

    /**
     * Posição efetivamente jogada na pelada. É separada da posição do cadastro
     * porque na pelada é comum um jogador de linha pegar o gol — e são as
     * defesas dele, não a ficha dele, que definem quais números valem.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "posicao_jogada", nullable = false, length = 20)
    private Posicao posicaoJogada;

    @Column(nullable = false)
    @Builder.Default
    private Integer gols = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer assistencias = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer desarmes = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer defesas = 0;

    @Column(name = "defesas_dificeis", nullable = false)
    @Builder.Default
    private Integer defesasDificeis = 0;

    @Column(name = "registrada_em", nullable = false)
    private LocalDateTime registradaEm;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    @PrePersist
    private void aoPersistir() {
        if (registradaEm == null) {
            registradaEm = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void aoAtualizar() {
        atualizadaEm = LocalDateTime.now();
    }

    /* ------------------------------------------------------------------
     * Comportamento de domínio
     * ------------------------------------------------------------------ */

    public ResumoEstatistico resumo() {
        return new ResumoEstatistico(
                zeroSeNulo(gols),
                zeroSeNulo(assistencias),
                zeroSeNulo(desarmes),
                zeroSeNulo(defesas),
                zeroSeNulo(defesasDificeis)
        );
    }

    public int pontuacao() {
        return resumo().pontuacao();
    }

    public boolean jogouNoGol() {
        return posicaoJogada == Posicao.GOLEIRO;
    }

    public Usuario getJogador() {
        return participacao != null ? participacao.getUsuario() : null;
    }

    public Pelada getPelada() {
        return participacao != null ? participacao.getPelada() : null;
    }

    private int zeroSeNulo(Integer valor) {
        return valor == null ? 0 : valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EstatisticaPartida outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
