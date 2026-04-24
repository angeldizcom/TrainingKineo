package com.app.kineo.service;

import com.app.kineo.config.mcp.AdaptiveContextBuilder;
import com.app.kineo.exception.AiEmptyResponseException;
import com.app.kineo.exception.PlanNotFoundException;
import com.app.kineo.exception.SessionAlreadyGeneratedTodayException;
import com.app.kineo.mapper.TrainingMapper;
import com.app.kineo.model.TrainingPlan;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.repository.TrainingPlanRepository;
import com.app.kineo.repository.TrainingSessionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class TrainingAiService {

    private final ChatClient                defaultChatClient;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingPlanRepository    planRepository;
    private final TrainingMapper            trainingMapper;
    private final AdaptiveContextBuilder    contextBuilder;
    private final AiResponseParser          responseParser;   // ← nuevo

    public TrainingAiService(ChatClient.Builder builder,
                             TrainingSessionRepository sessionRepository,
                             TrainingPlanRepository planRepository,
                             TrainingMapper trainingMapper,
                             AdaptiveContextBuilder contextBuilder,
                             AiResponseParser responseParser) {
        this.defaultChatClient = builder
                .defaultFunctions("getUserAssessment", "searchExercises")
                .build();
        this.sessionRepository = sessionRepository;
        this.planRepository    = planRepository;
        this.trainingMapper    = trainingMapper;
        this.contextBuilder    = contextBuilder;
        this.responseParser    = responseParser;
    }

    // ─────────────────────────────────────────────────────────
    // Generación estándar
    // ─────────────────────────────────────────────────────────

    public String generateAndSaveSession(Long userId, Long planId,
                                         String goal, ChatClient chatClient) {
        guardDuplicateToday(planId);

        // Llamada a la IA — respuesta raw sin parsear
        String rawResponse = chatClient.prompt()
                .user(u -> u.text("""
                        Genera una sesión de entrenamiento para el usuario {userId}.
                        Objetivo: {goal}.
                        Usa 'getUserAssessment' para lesiones y nivel.
                        Usa 'searchExercises' para el catálogo real.
                        Devuelve ÚNICAMENTE JSON válido con esta estructura:
                        { "sessionName": "...", "exercises": [
                            { "name": "...", "sets": 3, "reps": "10", "rpe": 7 }
                        ]}
                        """)
                        .param("userId", userId)
                        .param("goal", goal))
                .call()
                .content();

        // Parse con reintento automático si el JSON está malformado
        AiSessionResponse aiResponse = responseParser.parseWithRetry(
                rawResponse, AiSessionResponse.class, chatClient
        );

        validateAiResponse(aiResponse, "STANDARD");
        saveToDatabase(planId, aiResponse);
        return "Sesión '" + aiResponse.sessionName() + "' generada y guardada.";
    }

    // ─────────────────────────────────────────────────────────
    // Generación adaptativa
    // ─────────────────────────────────────────────────────────

    @Transactional
    public String generateAdaptiveSession(Long userId, Long planId,
                                          String goal, ChatClient chatClient) {
        guardDuplicateToday(planId);

        String adaptiveContext = contextBuilder.build(userId);
        log.info("[TrainingAiService] Contexto adaptativo construido para usuario {}.", userId);

        String rawResponse = chatClient.prompt()
                .user(u -> u.text("""
                        Eres un entrenador personal experto en periodización.
                        Genera la SIGUIENTE sesión para el usuario {userId}.
                        Objetivo: {goal}.

                        {adaptiveContext}

                        Usa 'getUserAssessment' para confirmar lesiones activas.
                        Usa 'searchExercises' para elegir ejercicios del catálogo real.
                        Aplica estrictamente las recomendaciones del contexto adaptativo.
                        Los nombres de ejercicios deben ser exactos y en español.
                        Devuelve ÚNICAMENTE JSON válido con esta estructura:
                        { "sessionName": "...", "exercises": [
                            { "name": "...", "sets": 3, "reps": "10", "rpe": 7 }
                        ]}
                        """)
                        .param("userId", userId)
                        .param("goal", goal != null ? goal : "progresión general")
                        .param("adaptiveContext", adaptiveContext))
                .call()
                .content();

        AiSessionResponse aiResponse = responseParser.parseWithRetry(
                rawResponse, AiSessionResponse.class, chatClient
        );

        validateAiResponse(aiResponse, "ADAPTIVE");
        saveToDatabase(planId, aiResponse);

        log.info("[TrainingAiService] Sesión adaptativa '{}' generada para usuario {}.",
                aiResponse.sessionName(), userId);

        return aiResponse.sessionName();
    }

    // ─────────────────────────────────────────────────────────
    // Persistencia
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void saveToDatabase(Long planId, AiSessionResponse aiResponse) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        TrainingSession session = trainingMapper.toTrainingSession(
                aiResponse, plan, LocalDate.now()
        );

        sessionRepository.save(session);

        log.info("[TrainingAiService] Sesión '{}' guardada con {} ejercicios.",
                session.getName(),
                session.getExercises() != null ? session.getExercises().size() : 0);
    }

    // ─────────────────────────────────────────────────────────
    // Guards
    // ─────────────────────────────────────────────────────────

    private void guardDuplicateToday(Long planId) {
        if (sessionRepository.existsByPlanIdAndScheduledDateToday(planId)) {
            throw new SessionAlreadyGeneratedTodayException(planId);
        }
    }

    private void validateAiResponse(AiSessionResponse response, String mode) {
        if (response == null
                || response.sessionName() == null
                || response.sessionName().isBlank()
                || response.exercises() == null
                || response.exercises().isEmpty()) {
            throw new AiEmptyResponseException(mode);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Contrato con la IA
    // ─────────────────────────────────────────────────────────

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