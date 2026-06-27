package com.hexabank.ledger.infrastructure.rest;

import com.hexabank.ledger.domain.exception.TransferViewNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio a respuestas HTTP usando {@link ProblemDetail} (RFC 7807).
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /** Proyección inexistente → 404 Not Found. */
    @ExceptionHandler(TransferViewNotFoundException.class)
    public ProblemDetail handleNotFound(TransferViewNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Parámetro inválido (p. ej. UUID mal formado) → 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
