package com.app.kineo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload que envía Android cuando el usuario termina una serie.
 *
 * Reglas de validación:
 * · actualReps: entre 1 y 200 si se proporciona (opcional)
 * · weight: entre 0 y 1000 kg si se proporciona (opcional, null = peso corporal)
 * · rpe: entre 1 y 10 si se proporciona (escala estándar de Borg)
 * · completed: obligatorio — define el estado final de la serie
 */
@Data
public class CompleteSetRequest {

    @Min(value = 1, message = "Las repeticiones reales deben ser al menos 1.")
    @Max(value = 200, message = "Las repeticiones reales no pueden superar 200.")
    private Integer actualReps;

    @DecimalMin(value = "0.0", inclusive = true, message = "El peso no puede ser negativo.")
    @DecimalMax(value = "1000.0", message = "El peso no puede superar 1000 kg.")
    private Double weight;

    @Min(value = 1, message = "El RPE mínimo es 1.")
    @Max(value = 10, message = "El RPE máximo es 10.")
    private Integer rpe;

    @NotNull(message = "El campo 'completed' es obligatorio.")
    private Boolean completed;
}