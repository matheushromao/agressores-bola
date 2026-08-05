package com.hmz.agressores_da_bola.repository;

import com.hmz.agressores_da_bola.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.repository.projection.TotaisJogador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstatisticaPartidaRepository extends JpaRepository<EstatisticaPartida, Long> {

    Optional<EstatisticaPartida> findByParticipacaoId(Long participacaoId);

    /**
     * Súmula completa de uma pelada. Traz jogador e pelada junto porque os
     * dois entram na resposta de cada linha.
     */
    @EntityGraph(attributePaths = {"participacao", "participacao.usuario", "participacao.pelada"})
    @Query("select e from EstatisticaPartida e where e.participacao.pelada.id = :peladaId")
    List<EstatisticaPartida> buscarDaPelada(@Param("peladaId") Long peladaId);

    /**
     * Soma as estatísticas de todas as peladas por jogador — a base de todos
     * os rankings. O agrupamento fica no banco (uma consulta só, sem carregar
     * o histórico inteiro na memória) e a conversão em pontos fica no domínio,
     * onde moram os pesos.
     *
     * @param peladaId quando informado, restringe o ranking a uma única
     *                 pelada; quando nulo, vale o histórico completo
     */
    @Query("""
            select new com.hmz.agressores_da_bola.repository.projection.TotaisJogador(
                u.id,
                u.nickname,
                u.nomeCompleto,
                u.posicao,
                u.estrelas,
                count(e.id),
                sum(e.gols),
                sum(e.assistencias),
                sum(e.desarmes),
                sum(e.defesas),
                sum(e.defesasDificeis))
            from EstatisticaPartida e
                join e.participacao p
                join p.usuario u
            where (:peladaId is null or p.pelada.id = :peladaId)
            group by u.id, u.nickname, u.nomeCompleto, u.posicao, u.estrelas
            """)
    List<TotaisJogador> somarPorJogador(@Param("peladaId") Long peladaId);
}
