package com.app.kineo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// ─────────────────────────────────────────────────────────────────────
// 1. PROGRESIÓN DE CARGA — evolución de un ejercicio a lo largo del tiempo
// ─────────────────────────────────────────────────────────────────────

/**
 * Evolución de rendimiento de un ejercicio concreto.
 * Cada punto representa la mejor serie de ese día (máximo volumen = peso × reps).
 * Es la base de las gráficas de progresión en Android.
 */
@Data
@Builder
public class ExerciseProgressDto {

    private Long   exerciseId;
    private String exerciseName;
    private String muscleGroup;

    /** Puntos temporales ordenados cronológicamente. */
    private List<ProgressPoint> history;

    /** Mejor marca histórica del usuario en este ejercicio. */
    private ProgressPoint personalBest;

    @Data
    @Builder
    public static class ProgressPoint {
        private LocalDate date;
        private Long      sessionId;
        private String    sessionName;
        private int       setNumber;
        private Integer   reps;
        private Double    weight;
        /** volumen = peso × reps. Si no hay peso (peso corporal) = reps. */
        private Double    volume;
        private Integer   rpe;
    }
}


// ─────────────────────────────────────────────────────────────────────
// 2. VOLUMEN SEMANAL — tonelaje por grupo muscular
// ─────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────
// 3. ADHERENCIA AL PLAN — sesiones completadas vs planificadas
// ─────────────────────────────────────────────────────────────────────

