package com.hmz.agressores_da_bola.repository.specification;

import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.model.enums.StatusPelada;
import com.hmz.agressores_da_bola.model.enums.TipoCampo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Filtros combináveis da listagem de peladas. Cada método cuida de um único
 * critério (SRP) e devolve {@link Specification#unrestricted()} quando o
 * filtro não foi informado — o elemento neutro do {@code allOf}, que não
 * restringe nada. Assim o service apenas empilha os critérios, sem uma
 * cascata de ifs.
 */
public final class PeladaSpecification {

    private PeladaSpecification() {
    }

    public static Specification<Pelada> comStatus(StatusPelada status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Pelada> comTipoCampo(TipoCampo tipoCampo) {
        if (tipoCampo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipoCampo"), tipoCampo);
    }

    public static Specification<Pelada> naCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            return Specification.unrestricted();
        }
        String termo = "%" + cidade.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("cidade")), termo);
    }

    public static Specification<Pelada> aPartirDe(LocalDate dataInicial) {
        if (dataInicial == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("data"), dataInicial);
    }

    public static Specification<Pelada> ate(LocalDate dataFinal) {
        if (dataFinal == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("data"), dataFinal);
    }

    public static Specification<Pelada> doOrganizador(Long organizadorId) {
        if (organizadorId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("organizador").get("id"), organizadorId);
    }

    /**
     * Peladas em que o usuário está na escalação. O join é seguro para
     * paginação porque a constraint única garante uma participação por
     * usuário em cada pelada — nenhuma linha duplicada.
     */
    public static Specification<Pelada> comParticipante(Long usuarioId) {
        if (usuarioId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.join("participacoes").get("usuario").get("id"), usuarioId);
    }
}
