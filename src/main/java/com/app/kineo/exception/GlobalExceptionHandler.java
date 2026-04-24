package com.app.kineo.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Punto único de manejo de excepciones para toda la API de Kineo.
 *
 * Captura en orden de prioridad:
 * 1. KineoException (negocio)              → status definido en la excepción
 * 2. MethodArgumentNotValidException       → 400, @Valid sobre @RequestBody
 * 3. ConstraintViolationException          → 400, @Validated sobre @RequestParam / @PathVariable
 * 4. MissingServletRequestParameterException → 400, parámetro obligatorio ausente
 * 5. MethodArgumentTypeMismatchException   → 400, tipo incorrecto en parámetro
 * 6. IllegalStateException                 → 409, estado ilegal de negocio
 * 7. Exception                             → 500, fallback genérico
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────
    // 1. Excepciones de negocio de Kineo
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(KineoException.class)
    public ResponseEntity<ErrorResponse> handleKineoException(
            KineoException ex, HttpServletRequest request) {

        if (ex.getStatus().is5xxServerError()) {
            log.error("[Kineo] Error de servidor: {}", ex.getMessage(), ex);
        } else {
            log.warn("[Kineo] Error de negocio [{}]: {}", ex.getStatus(), ex.getMessage());
        }

        return build(ex.getStatus(), ex.getMessage(), request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────────────────
    // 2. @Valid sobre @RequestBody
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleRequestBodyValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        log.warn("[Kineo] Validación @RequestBody fallida en {}: {} error(es).",
                request.getRequestURI(), fieldErrors.size());

        return build(HttpStatus.BAD_REQUEST,
                fieldErrors.size() + " campo(s) inválido(s).",
                request.getRequestURI(),
                fieldErrors);
    }

    // ─────────────────────────────────────────────────────────
    // 3. @Validated sobre @RequestParam / @PathVariable
    // ─────────────────────────────────────────────────────────

    /**
     * Se dispara cuando un @RequestParam o @PathVariable anotado con
     * @Min, @Max, @NotNull, @ValidProvider, etc. no supera la validación.
     *
     * Spring usa ConstraintViolationException en este caso (no
     * MethodArgumentNotValidException, que es solo para @RequestBody).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        // El path incluye "methodName.paramName" — extraemos solo el param
                        .field(extractParamName(cv))
                        .rejectedValue(cv.getInvalidValue())
                        .message(cv.getMessage())
                        .build())
                .toList();

        log.warn("[Kineo] Validación @RequestParam/@PathVariable fallida en {}: {} error(es).",
                request.getRequestURI(), fieldErrors.size());

        return build(HttpStatus.BAD_REQUEST,
                fieldErrors.size() + " parámetro(s) inválido(s).",
                request.getRequestURI(),
                fieldErrors);
    }

    // ─────────────────────────────────────────────────────────
    // 4. Parámetro obligatorio ausente
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("[Kineo] Parámetro requerido ausente: '{}' en {}.",
                ex.getParameterName(), request.getRequestURI());

        return build(HttpStatus.BAD_REQUEST,
                "Parámetro requerido ausente: '" + ex.getParameterName() + "'.",
                request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────────────────
    // 5. Tipo de parámetro incorrecto
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName() : "desconocido";

        String message = String.format(
                "El parámetro '%s' recibió '%s' pero se esperaba tipo %s.",
                ex.getName(), ex.getValue(), expected);

        log.warn("[Kineo] Tipo inválido en {}: {}", request.getRequestURI(), message);

        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────────────────
    // 6. Estado ilegal de negocio
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("[Kineo] Estado ilegal: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────────────────
    // 7. Fallback genérico
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("[Kineo] Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor. Por favor inténtalo de nuevo.",
                request.getRequestURI(), null);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                String path,
                                                List<ErrorResponse.FieldError> fieldErrors) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.name())
                        .message(message)
                        .path(path)
                        .fieldErrors(fieldErrors)
                        .build()
        );
    }

    /**
     * Extrae el nombre del parámetro del path de la violación.
     * El path tiene forma "methodName.parameterName" — devolvemos solo la última parte.
     */
    private String extractParamName(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : path;
    }
}