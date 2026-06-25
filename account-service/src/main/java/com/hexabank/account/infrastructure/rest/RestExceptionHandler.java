package com.hexabank.account.infrastructure.rest;

import com.hexabank.account.domain.exception.AccountNotFoundException;
import com.hexabank.account.domain.exception.InsufficientFundsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones de dominio a respuestas HTTP usando {@link ProblemDetail} (RFC 7807).
 *
 * <p>Es el punto donde la infraestructura "conoce" HTTP: el dominio lanza excepciones puras y aquí
 * se les asigna el código de estado. Centralizar esto evita repetir manejo de errores en cada
 * controller (DRY) y mantiene los controllers centrados en el camino feliz.</p>
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /** Cuenta inexistente → 404 Not Found. */
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleNotFound(AccountNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Violación de regla de negocio (fondos) → 409 Conflict. */
    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Datos de entrada inválidos (Bean Validation o invariantes de VO) → 400 Bad Request. */
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ProblemDetail handleValidation(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
