package com.hmz.agressores_da_bola.application.sorteio;

import com.hmz.agressores_da_bola.application.sorteio.dto.SorteioResponse;

/**
 * Contrato do sorteio de times. A operação não altera nada no banco: devolve
 * uma sugestão de divisão que o organizador aceita ou refaz. Só o
 * organizador da pelada pode sortear.
 */
public interface SorteioService {

    SorteioResponse sortear(Long peladaId, CriterioSorteio criterio, Long usuarioLogadoId);
}
