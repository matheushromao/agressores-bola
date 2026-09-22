package com.hmz.agressores_da_bola.application.sorteio;

import com.hmz.agressores_da_bola.application.sorteio.dto.JogadorSorteadoResponse;
import com.hmz.agressores_da_bola.application.sorteio.dto.SorteioResponse;
import com.hmz.agressores_da_bola.application.sorteio.dto.TimeSorteadoResponse;
import com.hmz.agressores_da_bola.application.usuario.UsuarioMapper;
import com.hmz.agressores_da_bola.domain.model.Pelada;
import com.hmz.agressores_da_bola.domain.sorteio.JogadorSorteavel;
import com.hmz.agressores_da_bola.domain.sorteio.TimesBalanceados;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SorteioMapper {

    /**
     * Nomes na ordem em que os times saem: Time A, Time B, Time C...
     */
    private static final char PRIMEIRA_LETRA = 'A';

    private final UsuarioMapper usuarioMapper;

    public SorteioResponse toResponse(Pelada pelada,
                                      TimesBalanceados balanceados,
                                      int jogadoresPorTime,
                                      int totalConfirmados,
                                      long semente) {

        List<TimeSorteadoResponse> times = new ArrayList<>(balanceados.times().size());
        for (int indice = 0; indice < balanceados.times().size(); indice++) {
            times.add(toTimeResponse(indice, balanceados.times().get(indice)));
        }

        return new SorteioResponse(
                pelada.getId(),
                pelada.getNome(),
                balanceados.times().size(),
                jogadoresPorTime,
                totalConfirmados,
                balanceados.diferencaEntreTimes(),
                times,
                toJogadoresResponse(balanceados.reservas()),
                semente
        );
    }

    private TimeSorteadoResponse toTimeResponse(int indice, List<JogadorSorteavel> time) {
        BigDecimal total = TimesBalanceados.somaEstrelas(time);

        return new TimeSorteadoResponse(
                "Time " + (char) (PRIMEIRA_LETRA + indice),
                time.size(),
                total,
                media(total, time.size()),
                time.stream().anyMatch(JogadorSorteavel::goleiro),
                toJogadoresResponse(time)
        );
    }

    private List<JogadorSorteadoResponse> toJogadoresResponse(List<JogadorSorteavel> jogadores) {
        return jogadores.stream()
                .sorted(TimesBalanceados.ordemDeExibicao())
                .map(jogador -> new JogadorSorteadoResponse(
                        usuarioMapper.toResumoResponse(jogador.usuario()),
                        jogador.goleiro()))
                .toList();
    }

    private BigDecimal media(BigDecimal total, int jogadores) {
        if (jogadores == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(jogadores), 2, RoundingMode.HALF_UP);
    }
}
