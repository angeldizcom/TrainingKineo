package com.app.kineo.service;

import com.app.kineo.model.*;
import com.app.kineo.repository.*;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TrainingAiService {

    private final ChatClient chatClient;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingPlanRepository planRepository;
    private final ExerciseRepository exerciseRepository;
    private final SessionExerciseRepository exerciseSessionRepository;
    private final ExerciseSetRepository exerciseSetRepository;


    public TrainingAiService(ChatClient.Builder builder,
                             TrainingSessionRepository sessionRepository,
                             ExerciseRepository exerciseRepository,
                             SessionExerciseRepository exerciseSessionRepository,
                             ExerciseSetRepository exerciseSetRepository,
                             TrainingPlanRepository planRepository) {
        // Configuramos el cliente con las funciones de MCP que registramos en el paso anterior
        this.chatClient = builder
                .defaultFunctions("getUserAssessment", "searchExercises")
                .build();
        this.sessionRepository = sessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseSessionRepository = exerciseSessionRepository;
        this.exerciseSetRepository = exerciseSetRepository;
        this.planRepository = planRepository;
    }

    public String generateAndSaveSession(Long userId, Long planId, String goal) {
        // 1. Invocamos a Gemini con el contexto del plan y el objetivo
        var converter = new BeanOutputConverter<>(AiSessionResponse.class);

        AiSessionResponse aiResponse = chatClient.prompt()
                .user(u -> u.text("""
                Genera una sesión de entrenamiento para el usuario {userId}.
                Objetivo: {goal}.
                Usa 'getUserAssessment' para las lesiones y nivel.
                Usa 'searchExercises' para el catálogo real.
                Formato de salida esperado: {format}
                """)
                        .param("userId", userId)
                        .param("goal", goal)
                        .param("format", converter.getFormat())) // Le pasamos el esquema JSON
                .call()
                .entity(converter);

        saveToDatabase(planId, aiResponse);

        return "Build completada y guardada en el sistema.";
    }

    @Transactional
    public void saveToDatabase(Long planId, AiSessionResponse aiResponse) {
        // 1. Localizar el Plan en el loot
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Error: Plan " + planId + " no detectado."));

        // 2. Crear la Sesión (Matrix -> Postgres)
        TrainingSession session = new TrainingSession();
        session.setTrainingPlan(plan);
        session.setName(aiResponse.sessionName());
        session.setScheduledDate(LocalDate.now());

        // Guardamos la sesión para generar la ID
        TrainingSession savedSession = sessionRepository.save(session);

        // 3. Procesar el arsenal de ejercicios de la IA
        for (int i = 0; i < aiResponse.exercises().size(); i++) {
            var aiEx = aiResponse.exercises().get(i);

            // Buscamos el ejercicio en tu catálogo por nombre
            Exercise realExercise = exerciseRepository.findByName(aiEx.name())
                    .orElse(null);

            if (realExercise != null) {
                // Creamos la relación Sesión <-> Ejercicio
                SessionExercise sessionEx = new SessionExercise();
                sessionEx.setTrainingSession(savedSession);
                sessionEx.setExercise(realExercise);
                sessionEx.setOrderIndex(i);

                SessionExercise savedSessionEx = exerciseSessionRepository.save(sessionEx);

                // 4. Generar los Sets (Series)
                for (int s = 1; s <= aiEx.sets(); s++) {
                    ExerciseSet set = new ExerciseSet();
                    set.setSessionExercise(savedSessionEx);
                    set.setSetNumber(s);
                    set.setTargetReps(Integer.parseInt(aiEx.reps().replaceAll("[^0-9]", "")));
                    set.setRpe(aiEx.rpe());
                    set.setCompleted(false);
                    exerciseSetRepository.save(set);
                }
            }
        }
        log.info("Build de entrenamiento guardada correctamente en PostgreSQL.");
    }

    // Helper para limpiar las reps (ej: de "12 reps" a 12)
    private Integer parseReps(String reps) {
        try {
            return Integer.parseInt(reps.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 10; // Valor por defecto si la IA se pone creativa con el texto
        }
    }
    public record AiSessionResponse(
            String sessionName,
            List<AiExercise> exercises
    ) {}

    public record AiExercise(
            String name,
            int sets,
            String reps,
            int rpe
    ) {}
}


