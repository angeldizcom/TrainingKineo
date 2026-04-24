package com.app.kineo.mapper;

import com.app.kineo.dto.AiTrainingPlanDto;
import com.app.kineo.dto.AiTrainingPlanDto.AiExerciseDto;
import com.app.kineo.dto.AiTrainingPlanDto.AiSessionDto;
import com.app.kineo.model.*;
import com.app.kineo.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mapper que transforma un {@link AiTrainingPlanDto} (respuesta estructurada de la IA)
 * en un grafo completo de entidades JPA listo para persistir con un único {@code save()}.
 *
 * <p><b>Política de ejercicios:</b> a diferencia del {@link TrainingMapper} (sesiones sueltas,
 * que omite ejercicios no encontrados), aquí se aplica <em>find-or-create</em>:
 * si el ejercicio no existe en catálogo se crea al vuelo para no dejar sesiones vacías
 * en un plan de semanas completas. La IA puede proponer ejercicios válidos que todavía
 * no están en el seed inicial.</p>
 *
 * <p><b>Cascade:</b> el grafo se persiste de raíz hacia hojas en una sola operación
 * gracias al {@code CascadeType.ALL} declarado en las entidades.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingPlanMapper {

    private final ExerciseRepository exerciseRepository;

    // ─────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────

    /**
     * Convierte el DTO de la IA en un {@link TrainingPlan} completo.
     *
     * @param aiPlan    DTO recibido del modelo (Gemini o Claude).
     * @param user      Propietario del plan.
     * @param startDate Fecha de inicio; las sesiones se distribuyen a partir de aquí.
     * @param weeks     Duración del plan en semanas (define {@code endDate}).
     * @return          Entidad raíz lista para {@code planRepository.save()}.
     */
    public TrainingPlan toPlan(AiTrainingPlanDto aiPlan,
                               User user,
                               LocalDate startDate,
                               int weeks) {

        TrainingPlan plan = buildPlan(aiPlan, user, startDate, weeks);
        List<TrainingSession> sessions = mapSessions(aiPlan.getSessions(), plan, startDate);
        plan.setSessions(sessions);

        int totalExercises = sessions.stream()
                .mapToInt(s -> s.getExercises() != null ? s.getExercises().size() : 0)
                .sum();

        log.info("[TrainingPlanMapper] Plan '{}' mapeado: {} sesiones, {} ejercicios totales.",
                plan.getName(), sessions.size(), totalExercises);

        return plan;
    }

    // ─────────────────────────────────────────────────────────
    // Construcción del plan raíz
    // ─────────────────────────────────────────────────────────

    private TrainingPlan buildPlan(AiTrainingPlanDto aiPlan,
                                   User user,
                                   LocalDate startDate,
                                   int weeks) {
        TrainingPlan plan = new TrainingPlan();
        plan.setUser(user);
        plan.setName(aiPlan.getName());
        plan.setGoal(aiPlan.getGoal());
        plan.setStartDate(startDate);
        plan.setEndDate(startDate.plusWeeks(weeks));
        plan.setStatus("ACTIVE");
        return plan;
    }

    // ─────────────────────────────────────────────────────────
    // Sesiones
    // ─────────────────────────────────────────────────────────

    /**
     * Distribuye las sesiones a lo largo del plan.
     * Cada sesión ocupa un día a partir de {@code startDate} (offset 0, 1, 2...).
     */
    private List<TrainingSession> mapSessions(List<AiSessionDto> aiSessions,
                                              TrainingPlan plan,
                                              LocalDate startDate) {
        if (aiSessions == null || aiSessions.isEmpty()) return List.of();

        List<TrainingSession> sessions = new ArrayList<>();
        int dayOffset = 0;

        for (AiSessionDto aiSession : aiSessions) {
            TrainingSession session = buildSession(aiSession, plan, startDate.plusDays(dayOffset++));
            List<SessionExercise> exercises = mapExercises(aiSession.getExercises(), session);
            session.setExercises(exercises);
            sessions.add(session);
        }

        return sessions;
    }

    private TrainingSession buildSession(AiSessionDto aiSession,
                                         TrainingPlan plan,
                                         LocalDate scheduledDate) {
        TrainingSession session = new TrainingSession();
        session.setTrainingPlan(plan);
        session.setName(aiSession.getDayName());
        session.setNotes(aiSession.getFocus());
        session.setScheduledDate(scheduledDate);
        return session;
    }

    // ─────────────────────────────────────────────────────────
    // Ejercicios y series
    // ─────────────────────────────────────────────────────────

    /**
     * Mapea ejercicios aplicando política find-or-create.
     * {@code orderIndex} es 1-based y continuo sobre todos los ejercicios de la sesión.
     */
    private List<SessionExercise> mapExercises(List<AiExerciseDto> aiExercises,
                                               TrainingSession session) {
        if (aiExercises == null || aiExercises.isEmpty()) return List.of();

        List<SessionExercise> result = new ArrayList<>();
        int orderIndex = 1;

        for (AiExerciseDto aiEx : aiExercises) {
            Exercise exercise = findOrCreate(aiEx);

            SessionExercise sessionExercise = new SessionExercise();
            sessionExercise.setTrainingSession(session);
            sessionExercise.setExercise(exercise);
            sessionExercise.setOrderIndex(orderIndex++);
            sessionExercise.setNotes(aiEx.getNotes());
            sessionExercise.setSets(buildSets(aiEx, sessionExercise));

            result.add(sessionExercise);
        }

        return result;
    }

    /**
     * Genera las series de un ejercicio.
     * {@code reps} en {@link AiExerciseDto} es {@code int}, no necesita parseo.
     */
    private List<ExerciseSet> buildSets(AiExerciseDto aiEx, SessionExercise sessionExercise) {
        List<ExerciseSet> sets = new ArrayList<>();

        for (int setNumber = 1; setNumber <= aiEx.getSets(); setNumber++) {
            ExerciseSet set = new ExerciseSet();
            set.setSessionExercise(sessionExercise);
            set.setSetNumber(setNumber);
            set.setTargetReps(aiEx.getReps());
            set.setCompleted(false);
            // weight / actualReps / rpe → null, los rellena el usuario al ejecutar
            sets.add(set);
        }

        return sets;
    }

    // ─────────────────────────────────────────────────────────
    // Find-or-create de ejercicios
    // ─────────────────────────────────────────────────────────

    /**
     * Busca el ejercicio en catálogo por nombre exacto.
     * Si no existe lo crea con los metadatos que la IA proporcionó.
     * Los nuevos ejercicios quedan marcados como "AI_GENERATED" en category
     * para que el equipo pueda revisarlos y enriquecerlos más tarde.
     */
    private Exercise findOrCreate(AiExerciseDto aiEx) {
        Optional<Exercise> existing = exerciseRepository.findByName(aiEx.getName());
        if (existing.isPresent()) {
            return existing.get();
        }

        log.warn("[TrainingPlanMapper] Ejercicio '{}' no encontrado en catálogo. " +
                "Se crea como AI_GENERATED para revisión posterior.", aiEx.getName());

        Exercise newEx = new Exercise();
        newEx.setName(aiEx.getName());
        newEx.setMuscleGroup(aiEx.getMuscleTarget());
        newEx.setCategory("AI_GENERATED");
        newEx.setDescription("Creado automáticamente por IA. Pendiente de revisión.");
        return exerciseRepository.save(newEx);
    }
}