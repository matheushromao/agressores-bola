package com.hmz.agressores_da_bola.model;

import com.hmz.agressores_da_bola.model.enums.Posicao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_usuarios")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

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
