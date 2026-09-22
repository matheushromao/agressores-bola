package com.hmz.agressores_da_bola.web.error;

import com.hmz.agressores_da_bola.application.usuario.CredenciaisInvalidasException;
import com.hmz.agressores_da_bola.domain.exception.AcessoNegadoException;
import com.hmz.agressores_da_bola.domain.exception.RecursoNaoEncontradoException;
import com.hmz.agressores_da_bola.domain.exception.RegraDeNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz as exceções do domínio e da aplicação no contrato único de erro da
 * API. É aqui, e só aqui, que "regra violada" vira 409 e "sem posse" vira 403.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return responder(HttpStatus.CONFLICT, "Regra de negócio violada", ex.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(AcessoNegadoException ex) {
        return responder(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.deCampos(
                        HttpStatus.BAD_REQUEST.value(),
                        "Dados inválidos",
                        "Um ou mais campos estão inválidos",
                        campos));
    }

    /**
     * Parâmetro de query com tipo errado — tipicamente um enum inexistente
     * em {@code ?status=...} ou uma data fora do formato ISO.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String valoresAceitos = valoresAceitos(ex.getRequiredType());
        String mensagem = "O parâmetro '" + ex.getName() + "' recebeu um valor inválido: " + ex.getValue()
                + (valoresAceitos == null ? "" : ". Valores aceitos: " + valoresAceitos);

        return responder(HttpStatus.BAD_REQUEST, "Parâmetro inválido", mensagem);
    }

    /**
     * Campo de ordenação inexistente em {@code ?sort=campoQueNaoExiste}.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> tratarOrdenacaoInvalida(PropertyReferenceException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Ordenação inválida",
                "Não existe o campo '" + ex.getPropertyName() + "' para ordenar o resultado");
    }

    /**
     * Corpo da requisição ilegível: JSON malformado ou enum desconhecido.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Requisição inválida",
                "O corpo da requisição não pôde ser lido. Verifique o formato dos campos e dos enums");
    }

    /**
     * Rede de segurança para as constraints do banco (unicidade, chave
     * estrangeira) que escapem das validações de negócio.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "Conflito de dados",
                "A operação viola uma restrição de integridade do banco de dados");
    }

    private static ResponseEntity<ErroResponse> responder(HttpStatus status, String erro, String mensagem) {
        return ResponseEntity.status(status).body(ErroResponse.de(status.value(), erro, mensagem));
    }

    private String valoresAceitos(Class<?> tipo) {
        if (tipo == null || !tipo.isEnum()) {
            return null;
        }
        StringBuilder valores = new StringBuilder();
        for (Object constante : tipo.getEnumConstants()) {
            if (!valores.isEmpty()) {
                valores.append(", ");
            }
            valores.append(constante);
        }
        return valores.toString();
    }
}
