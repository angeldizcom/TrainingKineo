package com.app.kineo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateAssessmentRequest {

    @Pattern(
        regexp = "LOSE_WEIGHT|GAIN_MUSCLE|IMPROVE_ENDURANCE|MAINTAIN_FITNESS",
        message = "Objetivo no válido."
    )
    private String primaryGoal;

    @Pattern(
        regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
        message = "Nivel no válido."
    )
    private String fitnessLevel;

    @Min(value = 1, message = "Mínimo 1 día por semana.")
    @Max(value = 7, message = "Máximo 7 días por semana.")
    private Integer trainingFrequency;

    @Min(15) @Max(180)
    private Integer preferredDurationMinutes;

    @Min(14) @Max(100)
    private Integer age;

    @DecimalMin("20.0") @DecimalMax("300.0")
    private Double weight;

    @DecimalMin("100.0") @DecimalMax("250.0")
    private Double height;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Género no válido.")
    private String gender;

    @Size(max = 500)
    private String injuries;

    @Pattern(
        regexp = "GYM|HOME_DUMBBELLS|BODYWEIGHT_ONLY",
        message = "Equipamiento no válido."
    )
    private String equipmentAccess;

    /**
     * Si true, genera un nuevo plan de entrenamiento adaptado
     * al assessment actualizado e invalida el anterior.
     * Default false — solo actualiza los datos sin generar plan.
     */
    private boolean regeneratePlan = false;
}
