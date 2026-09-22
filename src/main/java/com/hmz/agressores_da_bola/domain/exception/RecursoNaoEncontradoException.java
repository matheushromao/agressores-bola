package com.hmz.agressores_da_bola.domain.exception;

/**
 * O recurso pedido não existe. As fábricas nomeadas concentram as mensagens,
 * que antes eram repetidas em cada service que buscava a mesma coisa.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException pelada(Long id) {
        return new RecursoNaoEncontradoException("Pelada não encontrada com o id: " + id);
    }

    public static RecursoNaoEncontradoException usuario(Long id) {
        return new RecursoNaoEncontradoException("Usuário não encontrado com o id: " + id);
    }

    public static RecursoNaoEncontradoException usuarioComNickname(String nickname) {
        return new RecursoNaoEncontradoException("Usuário não encontrado com o nickname: " + nickname);
    }

    public static RecursoNaoEncontradoException participacao(Long peladaId, Long usuarioId) {
        return new RecursoNaoEncontradoException(
                "O usuário de id " + usuarioId + " não participa da pelada de id " + peladaId);
    }
}
