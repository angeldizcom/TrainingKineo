package com.app.kineo.repository;

import com.app.kineo.model.ExerciseSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {

    List<ExerciseSet> findBySessionExerciseIdOrderBySetNumberAsc(Long sessionExerciseId);

    // ─────────────────────────────────────────────────────────
    // Analytics — Progresión de carga
    // ─────────────────────────────────────────────────────────

    /**
     * Todas las series completadas de un ejercicio por un usuario,
     * ordenadas cronológicamente por fecha de sesión.
     *
     * Usado para construir la gráfica de progresión de carga.
     */
    @Query("""
            SELECT es FROM ExerciseSet es
            JOIN es.sessionExercise se
            JOIN se.trainingSession ts
            JOIN ts.trainingPlan tp
            WHERE tp.user.id    = :userId
              AND se.exercise.id = :exerciseId
              AND es.completed   = true
            ORDER BY ts.scheduledDate ASC, es.setNumber ASC
            """)
    List<ExerciseSet> findCompletedSetsByUserAndExercise(
            @Param("userId")     Long userId,
            @Param("exerciseId") Long exerciseId
    );

    // ─────────────────────────────────────────────────────────
    // Analytics — Volumen semanal
    // ─────────────────────────────────────────────────────────

    /**
     * Todas las series completadas de un usuario en las últimas N semanas,
     * con la información de grupo muscular y fecha de sesión necesaria
     * para agrupar el volumen por semana y músculo en memoria.
     *
     * Traemos las entidades completas en lugar de una proyección
     * para reutilizar la lógica de cálculo de volumen en el Service.
     */
    @Query("""
            SELECT es FROM ExerciseSet es
            JOIN FETCH es.sessionExercise se
            JOIN FETCH se.exercise e
            JOIN FETCH se.trainingSession ts
            JOIN ts.trainingPlan tp
            WHERE tp.user.id  = :userId
              AND es.completed = true
              AND ts.scheduledDate >= :since
            ORDER BY ts.scheduledDate ASC
            """)
    List<ExerciseSet> findCompletedSetsForVolumeAnalysis(
            @Param("userId") Long userId,
            @Param("since")  java.time.LocalDate since
    );
}