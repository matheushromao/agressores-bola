package com.hmz.agressores_da_bola.application.pelada;

import com.hmz.agressores_da_bola.application.common.PageResponse;
import com.hmz.agressores_da_bola.application.pelada.dto.PeladaResponse;
import com.hmz.agressores_da_bola.application.pelada.dto.PeladaResumoResponse;
import com.hmz.agressores_da_bola.domain.model.DadosPelada;
import com.hmz.agressores_da_bola.domain.model.enums.StatusPelada;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso do agendamento da pelada. A escalação tem contrato próprio,
 * {@link EscalacaoService}, para que quem só consulta ou agenda não dependa
 * das operações de elenco (Interface Segregation).
 *
 * <p>As operações de escrita recebem {@code usuarioLogadoId}, o usuário do
 * token: só o organizador altera a pelada.</p>
 */
public interface PeladaService {

    /**
     * @param usuarioLogadoId vira o organizador da pelada
     */
    PeladaResponse criar(DadosPelada dados, Long usuarioLogadoId);

    PeladaResponse buscarPorId(Long id);

    PageResponse<PeladaResumoResponse> listar(PeladaFiltro filtro, Pageable pageable);

    PeladaResponse atualizar(Long id, DadosPelada dados, Long usuarioLogadoId);

    PeladaResponse alterarStatus(Long id, StatusPelada novoStatus, Long usuarioLogadoId);

    void deletar(Long id, Long usuarioLogadoId);
}
