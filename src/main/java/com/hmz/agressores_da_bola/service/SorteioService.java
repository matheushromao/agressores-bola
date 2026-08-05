package com.hmz.agressores_da_bola.service;

import com.hmz.agressores_da_bola.dto.SorteioRequest;
import com.hmz.agressores_da_bola.dto.SorteioResponse;

/**
 * Contrato do sorteio de times. A operação não altera nada no banco: devolve
 * uma sugestão de divisão que o organizador aceita ou refaz.
 */
public interface SorteioService {

    SorteioResponse sortear(Long peladaId, SorteioRequest request);
}
