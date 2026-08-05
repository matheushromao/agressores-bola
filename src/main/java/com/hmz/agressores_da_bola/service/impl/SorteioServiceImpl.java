package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.SorteioRequest;
import com.hmz.agressores_da_bola.dto.SorteioResponse;
import com.hmz.agressores_da_bola.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.exception.RegraDeNegocioException;
import com.hmz.agressores_da_bola.mapper.SorteioMapper;
import com.hmz.agressores_da_bola.model.ParticipacaoPelada;
import com.hmz.agressores_da_bola.model.Pelada;
import com.hmz.agressores_da_bola.repository.PeladaRepository;
import com.hmz.agressores_da_bola.service.SorteioService;
import com.hmz.agressores_da_bola.service.sorteio.BalanceadorDeTimes;
import com.hmz.agressores_da_bola.service.sorteio.JogadorSorteavel;
import com.hmz.agressores_da_bola.service.sorteio.TimesBalanceados;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class SorteioServiceImpl implements SorteioService {

    private static final int MINIMO_DE_TIMES = 2;

    private final PeladaRepository peladaRepository;
    private final BalanceadorDeTimes balanceador;
    private final SorteioMapper sorteioMapper;

    @Override
    @Transactional(readOnly = true)
    public SorteioResponse sortear(Long peladaId, SorteioRequest request) {
        Pelada pelada = peladaRepository.buscarComParticipantes(peladaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pelada não encontrada com o id: " + peladaId));

        validarPeladaSorteavel(pelada);

        // Só quem confirmou presença entra no sorteio: convidado e lista de
        // espera ainda não são jogadores da pelada.
        List<JogadorSorteavel> confirmados = pelada.getParticipacoes().stream()
                .filter(ParticipacaoPelada::estaConfirmado)
                .map(participacao -> JogadorSorteavel.de(participacao.getUsuario()))
                .toList();

        int quantidadeTimes = resolverQuantidadeTimes(request, confirmados.size());
        int jogadoresPorTime = resolverJogadoresPorTime(request, confirmados.size(), quantidadeTimes);

        validarDivisao(confirmados.size(), quantidadeTimes, jogadoresPorTime);

        // Guardar a semente permite refazer exatamente o mesmo sorteio depois.
        long semente = request.semente() != null ? request.semente() : System.nanoTime();

        TimesBalanceados balanceados = balanceador.balancear(
                confirmados, quantidadeTimes, jogadoresPorTime, new Random(semente));

        return sorteioMapper.toResponse(
                pelada, balanceados, jogadoresPorTime, confirmados.size(), semente);
    }

    /* ------------------------------------------------------------------
     * Regras de negócio
     * ------------------------------------------------------------------ */

    private void validarPeladaSorteavel(Pelada pelada) {
        if (!pelada.getStatus().aceitaSorteio()) {
            throw new RegraDeNegocioException(
                    "A pelada está " + pelada.getStatus().getDescricao().toLowerCase()
                            + " e não faz mais sentido sortear times");
        }
    }

    /**
     * Quando o organizador pede times de tamanho fixo, a quantidade de times
     * é o que couber nos confirmados — o resto vira reserva.
     */
    private int resolverQuantidadeTimes(SorteioRequest request, int confirmados) {
        if (request.quantidadeTimes() != null) {
            return request.quantidadeTimes();
        }
        return confirmados / request.jogadoresPorTime();
    }

    private int resolverJogadoresPorTime(SorteioRequest request, int confirmados, int quantidadeTimes) {
        if (request.jogadoresPorTime() != null) {
            return request.jogadoresPorTime();
        }
        // Times de tamanho igual: os que sobram da divisão ficam de reserva,
        // porque um time com um jogador a mais já nasce em vantagem.
        return quantidadeTimes == 0 ? 0 : confirmados / quantidadeTimes;
    }

    private void validarDivisao(int confirmados, int quantidadeTimes, int jogadoresPorTime) {
        if (quantidadeTimes < MINIMO_DE_TIMES || jogadoresPorTime < MINIMO_DE_TIMES) {
            throw new RegraDeNegocioException(
                    "Não há confirmados suficientes para o sorteio pedido: são "
                            + confirmados + " jogadores, e é preciso pelo menos "
                            + (MINIMO_DE_TIMES * MINIMO_DE_TIMES)
                            + " para formar 2 times de 2");
        }
        if (confirmados < quantidadeTimes * jogadoresPorTime) {
            throw new RegraDeNegocioException(
                    "São necessários " + (quantidadeTimes * jogadoresPorTime)
                            + " jogadores confirmados para formar " + quantidadeTimes
                            + " times de " + jogadoresPorTime + ", mas a pelada tem "
                            + confirmados);
        }
    }
}
