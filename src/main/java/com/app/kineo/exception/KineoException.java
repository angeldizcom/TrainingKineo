package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción base de Kineo.
 * Todas las excepciones de negocio heredan de aquí para que el
 * GlobalExceptionHandler pueda tratarlas de forma uniforme.
 *
 * Cada subclase define su propio HttpStatus por defecto,
 * pero el handler puede sobreescribirlo si el contexto lo requiere.
 */
public abstract class KineoException extends RuntimeException {

    private final HttpStatus status;

    protected KineoException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    protected KineoException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}