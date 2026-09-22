package com.hmz.agressores_da_bola.application.sorteio;

import com.hmz.agressores_da_bola.application.port.PeladaRepository;
import com.hmz.agressores_da_bola.application.sorteio.dto.SorteioResponse;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.sorteio.BalanceadorDeTimes;
import com.hmz.agressores_da_bola.domain.sorteio.DivisaoDeTimes;
import com.hmz.agressores_da_bola.domain.sorteio.JogadorSorteavel;
import com.hmz.agressores_da_bola.domain.sorteio.TimesBalanceados;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SorteioServiceImpl implements SorteioService {

    private final PeladaRepository peladaRepository;
    private final BalanceadorDeTimes balanceador;
    private final SorteioMapper sorteioMapper;

    @Override
    @Transactional(readOnly = true)
    public SorteioResponse sortear(Long peladaId, CriterioSorteio criterio, Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.obterComParticipantes(peladaId);
        pelada.exigirOrganizador(usuarioLogadoId, "sortear os times desta pelada");
        pelada.garantirSorteavel();

        List<JogadorSorteavel> confirmados = pelada.jogadoresConfirmados().stream()
                .map(JogadorSorteavel::de)
                .toList();

        DivisaoDeTimes divisao = DivisaoDeTimes.resolver(
                criterio.quantidadeTimes(), criterio.jogadoresPorTime(), confirmados.size());

        // Guardar a semente permite refazer exatamente o mesmo sorteio depois.
        long semente = criterio.semente() != null ? criterio.semente() : System.nanoTime();

        TimesBalanceados balanceados = balanceador.balancear(
                confirmados, divisao.quantidadeTimes(), divisao.jogadoresPorTime(), new Random(semente));

        return sorteioMapper.toResponse(
                pelada, balanceados, divisao.jogadoresPorTime(), confirmados.size(), semente);
    }
}
