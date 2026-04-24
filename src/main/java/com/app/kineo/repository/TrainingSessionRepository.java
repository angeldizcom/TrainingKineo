package com.app.kineo.repository;

import com.app.kineo.model.TrainingSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    /** Para que la IA busque sesiones por usuario. */
    List<TrainingSession> findByTrainingPlanUserId(Long userId);

    // ─────────────────────────────────────────────────────────
    // Workout Execution — resuelve N+1
    // ─────────────────────────────────────────────────────────

    /**
     * Carga una sesión completa con TODOS sus hijos en UNA sola query.
     *
     * Sin esta query, acceder a session.getExercises() y luego a
     * se.getSets() desde SessionDetailDto.from() dispara:
     *   · 1 query para la sesión
     *   · 1 query por cada SessionExercise para cargar sus sets
     * → N+1 queries en total.
     *
     * Con JOIN FETCH anidado Hibernate genera un único SELECT con dos JOINs
     * y popula el grafo completo de una sola vez.
     *
     * DISTINCT es necesario porque el JOIN produce filas duplicadas de
     * TrainingSession cuando hay múltiples ejercicios y series.
     */
    @Query("""
            SELECT DISTINCT ts FROM TrainingSession ts
            LEFT JOIN FETCH ts.exercises se
            LEFT JOIN FETCH se.sets
            LEFT JOIN FETCH se.exercise
            WHERE ts.id = :sessionId
            """)
    Optional<TrainingSession> findByIdWithExercisesAndSets(@Param("sessionId") Long sessionId);

    // ─────────────────────────────────────────────────────────
    // Adherencia — ya existente, mantener
    // ─────────────────────────────────────────────────────────

    @Query("""
            SELECT DISTINCT ts FROM TrainingSession ts
            LEFT JOIN FETCH ts.exercises se
            LEFT JOIN FETCH se.sets
            WHERE ts.trainingPlan.id = :planId
            ORDER BY ts.scheduledDate ASC
            """)
    List<TrainingSession> findByPlanIdWithExercisesAndSets(@Param("planId") Long planId);

    // ─────────────────────────────────────────────────────────
    // Adaptive AI context — Fase 4
    // ─────────────────────────────────────────────────────────

    @Query("""
            SELECT DISTINCT ts FROM TrainingSession ts
            LEFT JOIN FETCH ts.exercises se
            LEFT JOIN FETCH se.sets
            LEFT JOIN FETCH se.exercise
            JOIN ts.trainingPlan tp
            WHERE tp.user.id = :userId
              AND ts.completedAt IS NOT NULL
            ORDER BY ts.completedAt DESC
            """)
    List<TrainingSession> findLastCompletedSessionsByUser(
            @Param("userId") Long userId,
            Pageable pageable
    );

    // ─────────────────────────────────────────────────────────
    // Idempotencia — guard de sesión duplicada hoy
    // ─────────────────────────────────────────────────────────

    /**
     * Comprueba si ya existe una sesión para este plan con scheduledDate = hoy.
     * Usado por TrainingAiService.guardDuplicateToday() antes de llamar a la IA.
     * Usa EXISTS en lugar de COUNT para cortar en cuanto encuentre una fila.
     */
    @Query("""
            SELECT COUNT(ts) > 0 FROM TrainingSession ts
            WHERE ts.trainingPlan.id = :planId
              AND ts.scheduledDate   = CURRENT_DATE
            """)
    boolean existsByPlanIdAndScheduledDateToday(@Param("planId") Long planId);
}