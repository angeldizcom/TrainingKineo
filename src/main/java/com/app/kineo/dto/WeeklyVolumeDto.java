package com.app.kineo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class WeeklyVolumeDto {

    /**
     * Semanas ordenadas de más reciente a más antigua.
     * Cada semana desglosa el volumen por grupo muscular.
     */
    private List<WeekSummary> weeks;

    @Data
    @Builder
    public static class WeekSummary {
        /** Lunes de la semana a la que corresponde este bloque. */
        private LocalDate weekStart;
        private List<MuscleVolume> byMuscleGroup;
        /** Volumen total de la semana (suma de todos los grupos). */
        private Double totalVolume;
        /** Número de sesiones completadas esa semana. */
        private int completedSessions;
    }

    @Data
    @Builder
    public static class MuscleVolume {
        private String muscleGroup;
        /** Suma de (actualReps × weight) de todas las series completadas. */
        private Double volume;
        /** Número de series totales trabajadas para este grupo. */
        private int    totalSets;
    }
}
