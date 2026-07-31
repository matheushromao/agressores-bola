package com.hmz.agressores_da_bola.exception;

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

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(HttpStatus.CONFLICT.value(), "Regra de negócio violada", ex.getMessage()));
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

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(HttpStatus.BAD_REQUEST.value(), "Parâmetro inválido", mensagem));
    }

    /**
     * Campo de ordenação inexistente em {@code ?sort=campoQueNaoExiste}.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResponse> tratarOrdenacaoInvalida(PropertyReferenceException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(HttpStatus.BAD_REQUEST.value(), "Ordenação inválida",
                        "Não existe o campo '" + ex.getPropertyName() + "' para ordenar o resultado"));
    }

    /**
     * Corpo da requisição ilegível: JSON malformado ou enum desconhecido.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.de(HttpStatus.BAD_REQUEST.value(), "Requisição inválida",
                        "O corpo da requisição não pôde ser lido. Verifique o formato dos campos e dos enums"));
    }

    /**
     * Rede de segurança para as constraints do banco (unicidade, chave
     * estrangeira) que escapem das validações de negócio.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(HttpStatus.CONFLICT.value(), "Conflito de dados",
                        "A operação viola uma restrição de integridade do banco de dados"));
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
