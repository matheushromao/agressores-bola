package com.hmz.agressores_da_bola.repository.specification;

import com.hmz.agressores_da_bola.model.Usuario;
import com.hmz.agressores_da_bola.model.enums.Posicao;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros combináveis da listagem de usuários. Mesmo contrato do
 * {@link PeladaSpecification}: filtro ausente vira
 * {@link Specification#unrestricted()}, que não restringe a consulta.
 */
public final class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> comPosicao(Posicao posicao) {
        if (posicao == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("posicao"), posicao);
    }

    public static Specification<Usuario> comNomeOuNickname(String termo) {
        if (termo == null || termo.isBlank()) {
            return Specification.unrestricted();
        }
        String busca = "%" + termo.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nomeCompleto")), busca),
                cb.like(cb.lower(root.get("nickname")), busca)
        );
    }

    public static Specification<Usuario> daNacionalidade(String nacionalidade) {
        if (nacionalidade == null || nacionalidade.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("nacionalidade")), nacionalidade.trim().toLowerCase());
    }
}
