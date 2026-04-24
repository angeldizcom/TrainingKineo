package com.app.kineo.service;

import com.app.kineo.dto.CompleteSetRequest;
import com.app.kineo.dto.SessionDetailDto;
import com.app.kineo.exception.SessionAlreadyCompletedException;
import com.app.kineo.exception.SessionNotFoundException;
import com.app.kineo.exception.SetNotFoundException;
import com.app.kineo.model.ExerciseSet;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.repository.ExerciseSetRepository;
import com.app.kineo.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutExecutionService {

    private final TrainingSessionRepository sessionRepository;
    private final ExerciseSetRepository     exerciseSetRepository;

    // ─────────────────────────────────────────────────────────
    // Consulta — 1 query, grafo completo
    // ─────────────────────────────────────────────────────────

    /**
     * Carga la sesión con todos sus ejercicios y series en una sola query
     * gracias a {@code findByIdWithExercisesAndSets}.
     *
     * Antes de este cambio: 1 + N queries (N = número de SessionExercise).
     * Después:              1 query con JOIN FETCH anidado.
     */
    @Transactional(readOnly = true)
    public SessionDetailDto getSessionDetail(Long sessionId) {
        return SessionDetailDto.from(findSessionWithGraphOrThrow(sessionId));
    }

    // ─────────────────────────────────────────────────────────
    // Ejecución
    // ─────────────────────────────────────────────────────────

    /**
     * Registra el resultado de una serie.
     *
     * Flujo:
     * 1. Carga la sesión con grafo completo para validar estado.
     * 2. Carga y actualiza la serie puntual (update de una sola fila).
     * 3. Recarga la sesión completa para calcular progressPercent actualizado.
     *
     * El paso 3 reutiliza la misma query con JOIN FETCH — no hay lazy loading.
     */
    @Transactional
    public SessionDetailDto completeSet(Long sessionId, Long setId, CompleteSetRequest request) {
        TrainingSession session = findSessionWithGraphOrThrow(sessionId);
        assertSessionNotCompleted(session);

        ExerciseSet set = exerciseSetRepository.findById(setId)
                .orElseThrow(() -> new SetNotFoundException(setId));

        applySetResults(set, request);
        exerciseSetRepository.save(set);

        log.info("[WorkoutExecution] Serie {} de sesión {} — reps: {}, peso: {}kg, RPE: {}",
                set.getSetNumber(), sessionId,
                request.getActualReps(), request.getWeight(), request.getRpe());

        // Recarga para que el DTO refleje el estado persistido actualizado
        return SessionDetailDto.from(findSessionWithGraphOrThrow(sessionId));
    }

    /**
     * Cierra la sesión. No necesita el grafo completo para actualizar
     * {@code completedAt}, pero sí para calcular el progressPercent final del DTO.
     */
    @Transactional
    public SessionDetailDto completeSession(Long sessionId) {
        TrainingSession session = findSessionWithGraphOrThrow(sessionId);
        assertSessionNotCompleted(session);

        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("[WorkoutExecution] Sesión '{}' completada.", session.getName());

        // Recarga para DTO con progressPercent final correcto
        return SessionDetailDto.from(findSessionWithGraphOrThrow(sessionId));
    }

    // ─────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────

    /**
     * Única entrada para cargar una sesión en este servicio.
     * Siempre usa la query con JOIN FETCH — nunca findById() simple.
     */
    private TrainingSession findSessionWithGraphOrThrow(Long sessionId) {
        return sessionRepository.findByIdWithExercisesAndSets(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    private void assertSessionNotCompleted(TrainingSession session) {
        if (session.getCompletedAt() != null) {
            throw new SessionAlreadyCompletedException(session.getName());
        }
    }

    private void applySetResults(ExerciseSet set, CompleteSetRequest request) {
        if (request.getActualReps() != null) set.setActualReps(request.getActualReps());
        if (request.getWeight()     != null) set.setWeight(request.getWeight());
        if (request.getRpe()        != null) set.setRpe(request.getRpe());
        set.setCompleted(request.getCompleted());
    }
}