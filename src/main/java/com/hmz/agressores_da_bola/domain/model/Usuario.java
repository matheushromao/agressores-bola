package com.hmz.agressores_da_bola.domain.model;

import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.model.enums.Posicao;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_usuarios")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /**
     * Hash da senha gerado pelo {@code CodificadorDeSenha} (BCrypt). Nunca é
     * exposto em DTO nem aceito na edição de perfil.
     */
    @Column(nullable = false, length = 100)
    private String senha;

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

    /* ------------------------------------------------------------------
     * Comportamento de domínio
     * ------------------------------------------------------------------ */

    /**
     * @param senhaCodificada hash já gerado; a entidade nunca vê a senha pura
     */
    public static Usuario cadastrar(PerfilUsuario perfil, String senhaCodificada) {
        Usuario usuario = new Usuario();
        usuario.senha = senhaCodificada;
        usuario.atualizarPerfil(perfil);
        return usuario;
    }

    /**
     * A senha não muda por aqui: a edição de perfil não mexe nela.
     */
    public void atualizarPerfil(PerfilUsuario perfil) {
        this.nomeCompleto = perfil.nomeCompleto();
        this.nickname = perfil.nickname();
        this.descricao = perfil.descricao();
        this.numeroCelular = perfil.numeroCelular();
        this.email = perfil.email();
        this.idade = perfil.idade();
        this.posicao = perfil.posicao();
        this.nacionalidade = perfil.nacionalidade();
        this.estrelas = perfil.estrelas();
    }

    /**
     * Posse do cadastro: cada jogador só altera e apaga o próprio perfil.
     */
    public void exigirProprio(Long usuarioLogadoId) {
        if (!id.equals(usuarioLogadoId)) {
            throw new AcessoNegadoException("Você só pode alterar o próprio cadastro");
        }
    }

    public boolean temNickname(String outroNickname) {
        return nickname.equalsIgnoreCase(outroNickname);
    }

    public boolean temEmail(String outroEmail) {
        return email.equalsIgnoreCase(outroEmail);
    }

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
