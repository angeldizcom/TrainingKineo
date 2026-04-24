package com.app.kineo.config.mcp;

import com.app.kineo.model.ExerciseSet;
import com.app.kineo.model.SessionExercise;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * Construye el bloque de contexto adaptativo que se inyecta en el prompt de la IA.
 *
 * <p>Analiza las últimas N sesiones completadas de un usuario y genera un resumen
 * estructurado con:</p>
 * <ul>
 *   <li>Ejercicios realizados, reps reales vs objetivo y RPE por sesión.</li>
 *   <li>Señales de fatiga (RPE alto) o margen de progresión (RPE bajo).</li>
 *   <li>Grupos musculares trabajados recientemente vs grupos sin estimular.</li>
 *   <li>Recomendaciones de ajuste que la IA debe respetar.</li>
 * </ul>
 *
 * <p><b>Claude</b> recibe este contexto en el prompt <em>y además</em> puede llamar
 * a {@code getRecentSessions} via MCP para obtener datos adicionales en tiempo real.<br>
 * <b>Gemini</b> solo recibe el contexto inline — es suficiente para generar
 * una sesión adaptada sin depender de herramientas externas.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdaptiveContextBuilder {

    private final TrainingSessionRepository sessionRepository;

    private static final int    RECENT_SESSIONS_LIMIT = 5;
    private static final int    RPE_FATIGUE_THRESHOLD = 8;   // RPE >= 8 → cerca del límite
    private static final int    RPE_PROGRESS_THRESHOLD = 6;  // RPE <= 6 → hay margen para progresar
    private static final int    DAYS_STALE_MUSCLE = 7;       // días sin trabajar un grupo = prioridad

    // ─────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────

    /**
     * Genera el bloque de texto de contexto adaptativo para un usuario.
     * Si el usuario no tiene sesiones completadas devuelve un contexto vacío
     * que indica que es la primera sesión (la IA genera sin restricciones).
     */
    public String build(Long userId) {
        List<TrainingSession> recent = sessionRepository
                .findLastCompletedSessionsByUser(userId, PageRequest.of(0, RECENT_SESSIONS_LIMIT));

        if (recent.isEmpty()) {
            return """
                    CONTEXTO ADAPTATIVO:
                    Es la primera sesión del usuario. No hay historial previo.
                    Genera una sesión de introducción con cargas conservadoras (RPE objetivo: 6-7).
                    """;
        }

        StringBuilder ctx = new StringBuilder();
        ctx.append("CONTEXTO ADAPTATIVO (usa esta información para ajustar la sesión):\n\n");

        appendSessionHistory(ctx, recent);
        appendMuscleStatus(ctx, recent);
        appendRecommendations(ctx, recent);

        String result = ctx.toString();
        log.info("[AdaptiveContext] Contexto generado para usuario {} ({} sesiones analizadas).",
                userId, recent.size());
        return result;
    }

    // ─────────────────────────────────────────────────────────
    // Bloques del contexto
    // ─────────────────────────────────────────────────────────

    private void appendSessionHistory(StringBuilder ctx, List<TrainingSession> sessions) {
        ctx.append("── HISTORIAL RECIENTE ──────────────────────────\n");

        for (TrainingSession session : sessions) {
            ctx.append(String.format("Sesión: \"%s\" (%s)\n",
                    session.getName(),
                    session.getScheduledDate()));

            if (session.getExercises() == null) continue;

            for (SessionExercise se : session.getExercises()) {
                if (se.getSets() == null || se.getSets().isEmpty()) continue;

                List<ExerciseSet> completedSets = se.getSets().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getCompleted()))
                        .toList();

                if (completedSets.isEmpty()) continue;

                double avgRpe    = completedSets.stream()
                        .filter(s -> s.getRpe() != null)
                        .mapToInt(ExerciseSet::getRpe)
                        .average().orElse(0);

                // Tomamos el peso de la última serie completada
                Double lastWeight = completedSets.stream()
                        .filter(s -> s.getWeight() != null)
                        .reduce((a, b) -> b)
                        .map(ExerciseSet::getWeight)
                        .orElse(null);

                int targetReps = completedSets.get(0).getTargetReps() != null
                        ? completedSets.get(0).getTargetReps() : 0;
                double avgActualReps = completedSets.stream()
                        .filter(s -> s.getActualReps() != null)
                        .mapToInt(ExerciseSet::getActualReps)
                        .average().orElse(0);

                String rpeSignal = rpeSignal(avgRpe);

                ctx.append(String.format(
                        "  · %-35s %d series | objetivo: %d reps | real: %.0f reps%s | RPE: %.1f %s\n",
                        se.getExercise().getName(),
                        completedSets.size(),
                        targetReps,
                        avgActualReps,
                        lastWeight != null ? " @ " + lastWeight + "kg" : "",
                        avgRpe,
                        rpeSignal
                ));
            }
            ctx.append("\n");
        }
    }

    private void appendMuscleStatus(StringBuilder ctx, List<TrainingSession> sessions) {
        ctx.append("── ESTADO POR GRUPO MUSCULAR ───────────────────\n");

        // Fecha más reciente en que se trabajó cada grupo
        Map<String, LocalDate> lastWorked = new HashMap<>();

        for (TrainingSession session : sessions) {
            if (session.getExercises() == null) continue;
            for (SessionExercise se : session.getExercises()) {
                String muscle = se.getExercise().getMuscleGroup();
                if (muscle == null) continue;
                lastWorked.merge(muscle, session.getScheduledDate(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        LocalDate today = LocalDate.now();
        lastWorked.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    long daysAgo = today.toEpochDay() - e.getValue().toEpochDay();
                    String status = daysAgo >= DAYS_STALE_MUSCLE
                            ? "⚠ SIN ESTIMULAR " + daysAgo + " días → PRIORIZAR"
                            : "OK (" + daysAgo + " días)";
                    ctx.append(String.format("  · %-12s %s\n", e.getKey(), status));
                });

        ctx.append("\n");
    }

    private void appendRecommendations(StringBuilder ctx, List<TrainingSession> sessions) {
        ctx.append("── RECOMENDACIONES DE AJUSTE ───────────────────\n");

        Map<String, List<Double>> rpeByExercise = new LinkedHashMap<>();

        for (TrainingSession session : sessions) {
            if (session.getExercises() == null) continue;
            for (SessionExercise se : session.getExercises()) {
                if (se.getSets() == null) continue;
                String exName = se.getExercise().getName();
                se.getSets().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getCompleted()) && s.getRpe() != null)
                        .forEach(s -> rpeByExercise
                                .computeIfAbsent(exName, k -> new ArrayList<>())
                                .add((double) s.getRpe()));
            }
        }

        boolean hasRec = false;
        for (Map.Entry<String, List<Double>> entry : rpeByExercise.entrySet()) {
            double avgRpe = entry.getValue().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            if (avgRpe >= RPE_FATIGUE_THRESHOLD) {
                ctx.append(String.format(
                        "  · %s → RPE alto (%.1f). Mantener o REDUCIR carga (-5%%). " +
                                "No aumentar volumen.\n", entry.getKey(), avgRpe));
                hasRec = true;
            } else if (avgRpe <= RPE_PROGRESS_THRESHOLD) {
                ctx.append(String.format(
                        "  · %s → RPE bajo (%.1f). El usuario tiene margen. " +
                                "AUMENTAR carga (+5%%) o añadir 1 serie.\n", entry.getKey(), avgRpe));
                hasRec = true;
            }
        }

        if (!hasRec) {
            ctx.append("  · Sin ajustes específicos. Mantener cargas y progresar gradualmente.\n");
        }

        ctx.append("""
 
                REGLAS QUE DEBES RESPETAR AL GENERAR LA SESIÓN:
                · Si un ejercicio tiene RPE >= 8 en el historial: NO aumentes la carga.
                · Si un ejercicio tiene RPE <= 6: puedes aumentar reps o peso un 5%.
                · Prioriza grupos musculares marcados como SIN ESTIMULAR.
                · El RPE objetivo de la sesión generada debe estar entre 7 y 8.
                """);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private String rpeSignal(double avgRpe) {
        if (avgRpe >= RPE_FATIGUE_THRESHOLD) return "→ CERCA DEL LÍMITE";
        if (avgRpe <= RPE_PROGRESS_THRESHOLD) return "→ MARGEN DE PROGRESIÓN";
        return "→ ZONA ÓPTIMA";
    }
}
