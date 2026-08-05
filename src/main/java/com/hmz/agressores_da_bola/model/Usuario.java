package com.hmz.agressores_da_bola.model;

import com.hmz.agressores_da_bola.model.enums.Posicao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_usuarios")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    /**
     * Nota usada quando o jogador ainda não foi avaliado: o meio da escala,
     * para não favorecer nem prejudicar o time que o receber no sorteio.
     */
    public static final BigDecimal ESTRELAS_PADRAO = new BigDecimal("3.0");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(length = 500)
    private String descricao;

    @Column(name = "numero_celular", nullable = false, length = 20)
    private String numeroCelular;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private Integer idade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Posicao posicao;

    @Column(nullable = false, length = 60)
    private String nacionalidade;

    /**
     * Nível técnico do jogador, de 1 a 5 estrelas com meia estrela de passo.
     * É a nota que o sorteio usa para equilibrar os times.
     *
     * <p>Aceita nulo no banco para não quebrar cadastros anteriores ao
     * atributo; quem não tem nota entra no sorteio como jogador mediano
     * através de {@link #estrelasOuPadrao()}.</p>
     */
    @Column(precision = 2, scale = 1)
    private BigDecimal estrelas;

    public BigDecimal estrelasOuPadrao() {
        return estrelas == null ? ESTRELAS_PADRAO : estrelas;
    }

    public boolean ehGoleiro() {
        return posicao == Posicao.GOLEIRO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario outro)) return false;
        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
