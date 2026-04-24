package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class AiParseException extends KineoException {
    public AiParseException(String rawResponse, Throwable cause) {
        super("La IA devolvió un formato inesperado y no se pudo procesar. " +
                        "Respuesta recibida: " + truncate(rawResponse),
                HttpStatus.UNPROCESSABLE_ENTITY, cause);
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }
}
