package com.app.kineo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo estandarizado para todas las respuestas de error de la API.
 *
 * Estructura fija que Android puede parsear de forma predecible
 * independientemente del tipo de error que haya ocurrido.
 *
 * Ejemplo de respuesta:
 * {
 *   "timestamp": "2025-04-23T10:15:00",
 *   "status": 404,
 *   "error": "NOT_FOUND",
 *   "message": "Sesión 99 no encontrada.",
 *   "path": "/api/workout/sessions/99",
 *   "fieldErrors": null          ← solo presente en errores de validación
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // omite campos null del JSON
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int           status;
    private final String        error;   // nombre del HttpStatus (NOT_FOUND, CONFLICT...)
    private final String        message; // mensaje legible para el desarrollador
    private final String        path;    // URI que causó el error

    /**
     * Solo presente en errores de validación (status 400).
     * Cada elemento identifica el campo inválido y el motivo.
     */
    private final List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final Object rejectedValue;
        private final String message;
    }
}
