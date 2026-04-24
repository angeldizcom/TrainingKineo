package com.app.kineo.service;

import com.app.kineo.dto.AiTrainingPlanDto;
import com.app.kineo.exception.NoAssessmentException;
import com.app.kineo.mapper.TrainingPlanMapper;
import com.app.kineo.model.TrainingPlan;
import com.app.kineo.model.User;
import com.app.kineo.model.UserAssessment;
import com.app.kineo.repository.TrainingPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final TrainingPlanMapper     planMapper;
    private final AiResponseParser       responseParser;   // ← nuevo
    private final ChatClient             defaultChatClient;

    public TrainingPlanService(ChatClient.Builder chatClientBuilder,
                               TrainingPlanRepository planRepository,
                               TrainingPlanMapper planMapper,
                               AiResponseParser responseParser) {
        this.defaultChatClient = chatClientBuilder.build();
        this.planRepository    = planRepository;
        this.planMapper        = planMapper;
        this.responseParser    = responseParser;
    }

    @Transactional
    public TrainingPlan generatePlanForUser(User user) {
        return generatePlanForUser(user, 4, defaultChatClient);
    }

    @Transactional
    public TrainingPlan generatePlanForUser(User user, int weeks, ChatClient chatClient) {
        UserAssessment assessment = user.getCurrentAssessment();
        if (assessment == null) {
            throw new NoAssessmentException(user.getId());
        }

        // Llamada a la IA — respuesta raw
        String rawResponse = chatClient.prompt()
                .user(buildPrompt(assessment, weeks))
                .call()
                .content();

        // Parse con reintento automático
        AiTrainingPlanDto aiPlan = responseParser.parseWithRetry(
                rawResponse, AiTrainingPlanDto.class, chatClient
        );

        deactivatePreviousActivePlan(user.getId());

        TrainingPlan plan = planMapper.toPlan(aiPlan, user, LocalDate.now(), weeks);
        TrainingPlan saved = planRepository.save(plan);

        log.info("[TrainingPlanService] Plan '{}' ({} semanas) generado para usuario {}.",
                saved.getName(), weeks, user.getId());

        return saved;
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void deactivatePreviousActivePlan(Long userId) {
        planRepository.findByUserIdAndStatus(userId, "ACTIVE").ifPresent(old -> {
            old.setStatus("COMPLETED");
            planRepository.save(old);
            log.info("[TrainingPlanService] Plan anterior '{}' desactivado.", old.getName());
        });
    }

    private String buildPrompt(UserAssessment a, int weeks) {
        return String.format("""
                Actúa como un entrenador personal experto. Crea un plan de entrenamiento de %d semana(s)
                para un cliente con el siguiente perfil:
                - Objetivo: %s | Nivel: %s | Días/semana: %d | Duración/sesión: %d min
                - Equipamiento: %s | Lesiones: %s
                - Género: %s | Edad: %d | Peso: %.1f kg | Altura: %.1f cm

                Genera exactamente %d sesiones. Nombres de ejercicios en español.
                Devuelve ÚNICAMENTE JSON válido sin markdown con esta estructura exacta:
                {
                  "name": "...", "goal": "...", "description": "...",
                  "sessions": [{
                    "dayName": "Día 1: Piernas", "focus": "Fuerza",
                    "exercises": [{
                      "name": "Sentadilla Trasera con Barra",
                      "muscleTarget": "LEGS", "sets": 4, "reps": 8, "notes": "..."
                    }]
                  }]
                }
                """,
                weeks,
                a.getPrimaryGoal(), a.getFitnessLevel(),
                a.getTrainingFrequency(), a.getPreferredDurationMinutes(),
                a.getEquipmentAccess(),
                a.getInjuries() != null ? a.getInjuries() : "Ninguna",
                a.getGender(), a.getAge(), a.getWeight(), a.getHeight(),
                a.getTrainingFrequency() * weeks
        );
    }
}