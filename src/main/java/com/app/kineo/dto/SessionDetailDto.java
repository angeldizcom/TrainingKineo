package com.app.kineo.dto;

import com.app.kineo.model.ExerciseSet;
import com.app.kineo.model.SessionExercise;
import com.app.kineo.model.TrainingSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ─────────────────────────────────────────────────────────────
// RESPONSES
// ─────────────────────────────────────────────────────────────

/**
 * Vista completa de una sesión para la pantalla de ejecución en Android.
 * Incluye ejercicios ordenados por orderIndex y sus series.
 */
@Data
@Builder
public class SessionDetailDto {

    private Long id;
    private String name;
    private LocalDate scheduledDate;
    private String notes;
    private boolean completed;
    private LocalDateTime completedAt;
    private List<SessionExerciseDto> exercises;

    /** % de series completadas sobre el total de la sesión */
    private int progressPercent;

    @Data
    @Builder
    public static class SessionExerciseDto {
        private Long sessionExerciseId;
        private Long exerciseId;
        private String exerciseName;
        private String muscleGroup;
        private String equipment;
        private String imageUrl;
        private int orderIndex;
        private String notes;
        private List<ExerciseSetDto> sets;
    }

    @Data
    @Builder
    public static class ExerciseSetDto {
        private Long setId;
        private int setNumber;
        private Integer targetReps;
        private Integer actualReps;
        private Double weight;
        private Integer rpe;
        private boolean completed;
    }

    // ── Factory ──────────────────────────────────────────────

    public static SessionDetailDto from(TrainingSession session) {
        List<SessionExerciseDto> exerciseDtos = session.getExercises() == null
                ? List.of()
                : session.getExercises().stream()
                        .sorted((a, b) -> Integer.compare(
                                a.getOrderIndex() != null ? a.getOrderIndex() : 0,
                                b.getOrderIndex() != null ? b.getOrderIndex() : 0))
                        .map(SessionDetailDto::toExerciseDto)
                        .toList();

        int totalSets    = exerciseDtos.stream().mapToInt(e -> e.getSets().size()).sum();
        int doneSets     = exerciseDtos.stream()
                .flatMap(e -> e.getSets().stream())
                .mapToInt(s -> s.isCompleted() ? 1 : 0)
                .sum();
        int progress     = totalSets == 0 ? 0 : (doneSets * 100 / totalSets);

        return SessionDetailDto.builder()
                .id(session.getId())
                .name(session.getName())
                .scheduledDate(session.getScheduledDate())
                .notes(session.getNotes())
                .completed(session.getCompletedAt() != null)
                .completedAt(session.getCompletedAt())
                .exercises(exerciseDtos)
                .progressPercent(progress)
                .build();
    }

    private static SessionExerciseDto toExerciseDto(SessionExercise se) {
        List<ExerciseSetDto> setDtos = se.getSets() == null
                ? List.of()
                : se.getSets().stream()
                        .sorted((a, b) -> Integer.compare(a.getSetNumber(), b.getSetNumber()))
                        .map(SessionDetailDto::toSetDto)
                        .toList();

        return SessionExerciseDto.builder()
                .sessionExerciseId(se.getId())
                .exerciseId(se.getExercise().getId())
                .exerciseName(se.getExercise().getName())
                .muscleGroup(se.getExercise().getMuscleGroup())
                .equipment(se.getExercise().getEquipment())
                .imageUrl(se.getExercise().getImageUrl())
                .orderIndex(se.getOrderIndex() != null ? se.getOrderIndex() : 0)
                .notes(se.getNotes())
                .sets(setDtos)
                .build();
    }

    private static ExerciseSetDto toSetDto(ExerciseSet set) {
        return ExerciseSetDto.builder()
                .setId(set.getId())
                .setNumber(set.getSetNumber())
                .targetReps(set.getTargetReps())
                .actualReps(set.getActualReps())
                .weight(set.getWeight())
                .rpe(set.getRpe())
                .completed(Boolean.TRUE.equals(set.getCompleted()))
                .build();
    }
}
