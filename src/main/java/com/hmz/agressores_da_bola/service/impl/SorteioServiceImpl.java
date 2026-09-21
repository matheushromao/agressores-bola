package com.hmz.agressores_da_bola.service.impl;

import com.hmz.agressores_da_bola.dto.SorteioRequest;
import com.hmz.agressores_da_bola.dto.SorteioResponse;
import com.hmz.agressores_da_bola.exception.AcessoNegadoException;
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
    private static final int MINIMO_DE_JOGADORES_POR_TIME = 2;

    private final PeladaRepository peladaRepository;
    private final BalanceadorDeTimes balanceador;
    private final SorteioMapper sorteioMapper;

    @Override
    @Transactional(readOnly = true)
    public SorteioResponse sortear(Long peladaId, SorteioRequest request, Long usuarioLogadoId) {
        Pelada pelada = peladaRepository.buscarComParticipantes(peladaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pelada não encontrada com o id: " + peladaId));

        if (!pelada.organizadaPor(usuarioLogadoId)) {
            throw new AcessoNegadoException("Só o organizador pode sortear os times desta pelada");
        }
        validarPeladaSorteavel(pelada);

        // Só quem confirmou presença entra no sorteio: convidado e lista de
        // espera ainda não são jogadores da pelada.
        List<JogadorSorteavel> confirmados = pelada.getParticipacoes().stream()
                .filter(ParticipacaoPelada::estaConfirmado)
                .map(participacao -> JogadorSorteavel.de(participacao.getUsuario()))
                .toList();

        int quantidadeTimes = resolverQuantidadeTimes(request, confirmados.size());
        int jogadoresPorTime = resolverJogadoresPorTime(request, confirmados.size(), quantidadeTimes);

        validarDivisao(request, confirmados.size(), quantidadeTimes, jogadoresPorTime);

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

    /**
     * Só existe um jeito de a divisão não fechar: faltar gente para o critério
     * pedido. O outro caso imaginável — confirmados a menos do que
     * {@code times × jogadoresPorTime} — não acontece, porque o valor deduzido
     * sempre vem de uma divisão inteira e o resto vira reserva.
     *
     * <p>A mensagem cita <strong>o que o organizador pediu</strong>, não o
     * mínimo absoluto do sorteio: quem pede times de 6 com 10 confirmados
     * precisa ouvir que faltam 2 jogadores, e não que "é preciso pelo menos 4
     * para formar 2 times de 2" — verdadeiro, porém inútil.
     */
    private void validarDivisao(SorteioRequest request, int confirmados,
                                int quantidadeTimes, int jogadoresPorTime) {
        if (quantidadeTimes >= MINIMO_DE_TIMES && jogadoresPorTime >= MINIMO_DE_JOGADORES_POR_TIME) {
            return;
        }

        if (request.quantidadeTimes() != null) {
            int necessarios = request.quantidadeTimes() * MINIMO_DE_JOGADORES_POR_TIME;
            throw new RegraDeNegocioException(
                    "Para formar " + request.quantidadeTimes() + " times são necessários pelo menos "
                            + necessarios + " jogadores confirmados, já que cada time precisa de "
                            + MINIMO_DE_JOGADORES_POR_TIME + ", mas a pelada tem " + confirmados);
        }

        int necessarios = request.jogadoresPorTime() * MINIMO_DE_TIMES;
        throw new RegraDeNegocioException(
                "Para formar times de " + request.jogadoresPorTime()
                        + " jogadores são necessários pelo menos " + necessarios
                        + " confirmados, já que o sorteio precisa de ao menos "
                        + MINIMO_DE_TIMES + " times, mas a pelada tem " + confirmados);
    }
}
