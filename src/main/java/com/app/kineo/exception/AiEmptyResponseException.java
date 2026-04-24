package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class AiEmptyResponseException extends KineoException {
    public AiEmptyResponseException(String provider) {
        super("El modelo " + provider + " devolvió una respuesta vacía. Inténtalo de nuevo.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
