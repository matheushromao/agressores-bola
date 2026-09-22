package com.hmz.agressores_da_bola.infrastructure.persistence;

import com.hmz.agressores_da_bola.application.port.EstatisticaRepository;
import com.hmz.agressores_da_bola.domain.model.EstatisticaPartida;
import com.hmz.agressores_da_bola.domain.ranking.TotaisJogador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class EstatisticaRepositoryAdapter implements EstatisticaRepository {

    private final EstatisticaJpaRepository jpa;

    @Override
    public Optional<EstatisticaPartida> buscarDaParticipacao(Long participacaoId) {
        return jpa.findByParticipacaoId(participacaoId);
    }

    @Override
    public List<EstatisticaPartida> listarDaPelada(Long peladaId) {
        return jpa.buscarDaPelada(peladaId);
    }

    @Override
    public List<TotaisJogador> somarPorJogador(Long peladaId) {
        return jpa.somarPorJogador(peladaId);
    }

    @Override
    public EstatisticaPartida salvar(EstatisticaPartida estatistica) {
        return jpa.save(estatistica);
    }
}
