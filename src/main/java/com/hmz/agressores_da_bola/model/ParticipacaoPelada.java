package com.hmz.agressores_da_bola.model;

import com.hmz.agressores_da_bola.model.enums.StatusParticipacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipacaoPelada {

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

    @PrePersist
    private void aoPersistir() {
        if (dataInscricao == null) {
            dataInscricao = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusParticipacao.CONVIDADO;
        }
    }

    public boolean pertenceAoUsuario(Long usuarioId) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(usuarioId);
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
