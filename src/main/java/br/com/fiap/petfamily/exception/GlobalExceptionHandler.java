package br.com.fiap.petfamily.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleRotaInexistente(Exception ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Not Found", "Rota não encontrada: " + req.getRequestURI(), req);
    }

    @ExceptionHandler({EmailJaCadastradoException.class, ConflitoOperacaoException.class})
    public ResponseEntity<ErrorResponse> handleConflito(RuntimeException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Conflito",
                "Operação viola uma restrição de integridade dos dados (registro duplicado ou em uso).", req);
    }

    @ExceptionHandler(OperacaoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleOperacaoInvalida(OperacaoInvalidaException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Operação Inválida", ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, "Não autenticado", "E-mail ou senha inválidos.", req);
    }

    @ExceptionHandler(TentativasExcedidasException.class)
    public ResponseEntity<ErrorResponse> handleTentativasExcedidas(TentativasExcedidasException ex, HttpServletRequest req) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas", ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método Não Permitido",
                "Método " + ex.getMethod() + " não é suportado para " + req.getRequestURI() + ".", req);
    }

    // JSON malformado, data/horário inválido, enum desconhecido: erro do cliente => 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMensagemIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Bad Request",
                "Corpo da requisição inválido ou malformado (verifique o JSON e os formatos de data/hora).", req);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleOrdenacaoInvalida(PropertyReferenceException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Bad Request",
                "Campo de ordenação inválido: " + ex.getPropertyName(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Bad Request", "Parâmetro inválido: " + ex.getName(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> campos.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        String resumo = campos.values().stream().collect(Collectors.joining("; "));

        ErrorResponse corpo = ErrorResponse.of(HttpStatus.BAD_REQUEST, "Validation Error", resumo, req.getRequestURI());
        corpo.setCampos(campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Erro não tratado em {}", req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", req);
    }

    /** Único ponto que monta a resposta de erro — evita repetir o builder em cada handler. */
    private ResponseEntity<ErrorResponse> resposta(HttpStatus status, String titulo, String mensagem,
                                                   HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, titulo, mensagem, req.getRequestURI()));
    }
}