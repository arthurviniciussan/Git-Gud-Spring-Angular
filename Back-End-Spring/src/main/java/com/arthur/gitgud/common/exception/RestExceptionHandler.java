package com.arthur.gitgud.common.exception;

import com.arthur.gitgud.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Tratamento central de erros.
 *
 * <p>Trata apenas as excecoes base de {@code common.exception}. Quando um modulo
 * novo entrar (artigos, imagens), ele estende a base adequada e este handler nao
 * muda — nao conhece modulo nenhum.
 *
 * <p>Estende {@code ResponseEntityExceptionHandler} para herdar o mapeamento que
 * o Spring ja faz das falhas de MVC (rota inexistente, metodo errado, corpo
 * malformado). Sem isso elas cairiam no {@code Exception} generico e um 404
 * viraria 500.
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Mensagem unica para qualquer falha de autenticacao.
     *
     * <p>Senha errada e email inexistente respondem exatamente isto, com o mesmo
     * status — nao da para descobrir quais emails existem tentando logar.
     */
    private static final String CREDENCIAIS_INVALIDAS = "Credenciais invalidas.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
        return resposta(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        // O motivo real vai para o log; o cliente recebe sempre a mesma frase.
        log.debug("Falha de autenticacao: {}", e.getMessage());
        return resposta(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
    }

    /** Rede de seguranca: o stack vai para o log, a resposta nao revela nada. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Erro nao tratado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.");
    }

    /** Campo invalido no corpo: diz qual campo e por que, sem expor a excecao. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders cabecalhos,
            HttpStatusCode status, WebRequest requisicao) {

        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(status).body(ErrorResponse.of(mensagem));
    }

    /**
     * Converte o {@code ProblemDetail} que o Spring monta para as falhas de MVC
     * no nosso formato — assim a API tem um corpo de erro so, em qualquer status.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object corpo, HttpHeaders cabecalhos, HttpStatusCode status, WebRequest requisicao) {

        String mensagem = corpo instanceof ProblemDetail detalhe && detalhe.getDetail() != null
                ? detalhe.getDetail()
                : HttpStatus.valueOf(status.value()).getReasonPhrase();

        return ResponseEntity.status(status).headers(cabecalhos).body(ErrorResponse.of(mensagem));
    }

    private ResponseEntity<ErrorResponse> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(ErrorResponse.of(mensagem));
    }
}
