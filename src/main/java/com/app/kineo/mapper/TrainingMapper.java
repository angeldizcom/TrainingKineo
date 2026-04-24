package com.app.kineo.mapper;

import com.app.kineo.model.Exercise;
import com.app.kineo.model.ExerciseSet;
import com.app.kineo.model.SessionExercise;
import com.app.kineo.model.TrainingPlan;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.repository.ExerciseRepository;
import com.app.kineo.service.TrainingAiService.AiExercise;
import com.app.kineo.service.TrainingAiService.AiSessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mapper responsable de transformar el DTO de la IA ({@link AiSessionResponse})
 * en un grafo de entidades JPA ({@link TrainingSession}) listo para ser
 * persistido por el repositorio con una única llamada a {@code save()}.
 *
 * <p>Gracias al {@code CascadeType.ALL} definido en las entidades, guardar
 * la {@code TrainingSession} raíz propagará automáticamente la inserción de
 * {@link SessionExercise} y {@link ExerciseSet}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingMapper {

    private final ExerciseRepository exerciseRepository;

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Punto de entrada principal. Convierte una respuesta de la IA en una
     * {@link TrainingSession} completa con sus ejercicios y series anidados.
     *
     * @param aiResponse    Respuesta estructurada devuelta por el modelo de IA.
     * @param plan          Plan de entrenamiento al que pertenece la sesión.
     * @param scheduledDate Fecha en que se planifica la sesión.
     * @return              Entidad raíz lista para persistir (sin ID asignado todavía).
     */
    public TrainingSession toTrainingSession(AiSessionResponse aiResponse,
                                             TrainingPlan plan,
                                             LocalDate scheduledDate) {

        TrainingSession session = buildSession(aiResponse, plan, scheduledDate);
        List<SessionExercise> sessionExercises = mapExercises(aiResponse.exercises(), session);
        session.setExercises(sessionExercises);

        log.info("[TrainingMapper] Sesión '{}' mapeada con {}/{} ejercicios válidos del catálogo.",
                session.getName(),
                sessionExercises.size(),
                aiResponse.exercises().size());

        return session;
    }

    // -------------------------------------------------------------------------
    // Métodos privados de construcción
    // -------------------------------------------------------------------------

    /** Construye la entidad raíz TrainingSession a partir de los metadatos de la IA. */
    private TrainingSession buildSession(AiSessionResponse aiResponse,
                                         TrainingPlan plan,
                                         LocalDate scheduledDate) {
        TrainingSession session = new TrainingSession();
        session.setTrainingPlan(plan);
        session.setName(aiResponse.sessionName());
        session.setScheduledDate(scheduledDate);
        return session;
    }

    /**
     * Itera la lista de ejercicios de la IA.
     * <ul>
     *   <li>Busca cada ejercicio en el catálogo por nombre exacto.</li>
     *   <li>Asigna {@code orderIndex} comenzando en 1 (sólo ejercicios encontrados).</li>
     *   <li>Si un ejercicio NO existe en catálogo, se registra un warning y se omite.</li>
     * </ul>
     */
    private List<SessionExercise> mapExercises(List<AiExercise> aiExercises,
                                               TrainingSession session) {
        List<SessionExercise> result = new ArrayList<>();
        int orderIndex = 1;

        for (AiExercise aiEx : aiExercises) {
            Optional<Exercise> exerciseOpt = exerciseRepository.findByName(aiEx.name());

            if (exerciseOpt.isEmpty()) {
                log.warn("[TrainingMapper] Ejercicio '{}' no encontrado en el catálogo. " +
                        "Se omite de la sesión '{}'.", aiEx.name(), session.getName());
                continue;
            }

            SessionExercise sessionExercise = buildSessionExercise(
                    aiEx, exerciseOpt.get(), session, orderIndex++
            );
            result.add(sessionExercise);
        }

        return result;
    }

    /**
     * Construye un {@link SessionExercise} con sus {@link ExerciseSet} anidados.
     * El {@code orderIndex} refleja la posición real dentro de los ejercicios
     * válidos encontrados en catálogo (saltar inválidos no rompe la secuencia).
     */
    private SessionExercise buildSessionExercise(AiExercise aiEx,
                                                 Exercise exercise,
                                                 TrainingSession session,
                                                 int orderIndex) {
        SessionExercise sessionExercise = new SessionExercise();
        sessionExercise.setTrainingSession(session);   // lado propietario de la FK
        sessionExercise.setExercise(exercise);
        sessionExercise.setOrderIndex(orderIndex);

        List<ExerciseSet> sets = buildSets(aiEx, sessionExercise);
        sessionExercise.setSets(sets);

        return sessionExercise;
    }

    /**
     * Genera los {@link ExerciseSet} para cada serie indicada por la IA.
     * {@code setNumber} es 1-based y continuo (1, 2, 3...).
     * Los campos de rendimiento real ({@code actualReps}, {@code weight}) se dejan
     * en {@code null}: son completados por el usuario durante la ejecución.
     */
    private List<ExerciseSet> buildSets(AiExercise aiEx, SessionExercise sessionExercise) {
        List<ExerciseSet> sets = new ArrayList<>();
        int targetReps = parseReps(aiEx.reps());

        for (int setNumber = 1; setNumber <= aiEx.sets(); setNumber++) {
            ExerciseSet set = new ExerciseSet();
            set.setSessionExercise(sessionExercise); // lado propietario de la FK
            set.setSetNumber(setNumber);
            set.setTargetReps(targetReps);
            set.setRpe(aiEx.rpe());
            set.setCompleted(false);
            // actualReps / weight: null → pendiente de completar por el usuario
            sets.add(set);
        }

        return sets;
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    /**
     * Parsea de forma segura el campo {@code reps} que la IA puede devolver
     * como texto libre ("12 reps", "8-10", "12").
     * Extrae el primer número encontrado; si no hay ninguno usa 10 como fallback.
     */
    private int parseReps(String reps) {
        if (reps == null || reps.isBlank()) {
            log.warn("[TrainingMapper] Campo 'reps' vacío, usando valor por defecto: 10.");
            return 10;
        }
        try {
            // Toma solo los dígitos y parsea el primer número (e.g. "8-10" → 8)
            String digits = reps.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 10 : Integer.parseInt(digits.substring(0, Math.min(digits.length(), 3)));
        } catch (NumberFormatException e) {
            log.warn("[TrainingMapper] No se pudo parsear '{}' como reps. Usando valor por defecto: 10.", reps);
            return 10;
        }
    }
}