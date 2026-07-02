package com.hexabank.transfer.infrastructure.rest;

import com.hexabank.transfer.domain.exception.AccountNotFoundException;
import com.hexabank.transfer.domain.exception.InvalidTransferStateException;
import com.hexabank.transfer.domain.exception.TransferNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio a respuestas HTTP usando {@link ProblemDetail} (RFC 7807).
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /** Transferencia o cuenta inexistente (o ajena al llamante) → 404 Not Found. */
    @ExceptionHandler({TransferNotFoundException.class, AccountNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Transición de estado no permitida → 409 Conflict. */
    @ExceptionHandler(InvalidTransferStateException.class)
    public ProblemDetail handleInvalidState(InvalidTransferStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Datos de entrada inválidos → 400 Bad Request. */
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ProblemDetail handleValidation(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
