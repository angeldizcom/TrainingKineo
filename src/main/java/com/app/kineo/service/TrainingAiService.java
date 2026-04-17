package com.app.kineo.service;

import com.app.kineo.model.TrainingPlan;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.repository.TrainingPlanRepository;
import com.app.kineo.repository.TrainingSessionRepository;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
public class TrainingAiService {

    private final ChatClient chatClient;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingPlanRepository planRepository;

    public TrainingAiService(ChatClient.Builder builder,
                             TrainingSessionRepository sessionRepository,
                             TrainingPlanRepository planRepository) {
        // Configuramos el cliente con las funciones de MCP que registramos en el paso anterior
        this.chatClient = builder
                .defaultFunctions("getUserAssessment", "searchExercises")
                .build();
        this.sessionRepository = sessionRepository;
        this.planRepository = planRepository;
    }

    public String generateAndSaveSession(Long userId, Long planId, String goal) {
        // 1. Invocamos a Gemini con el contexto del plan y el objetivo
        String prompt = """
            Genera una sesión de entrenamiento (TrainingSession) para el usuario %s.
            El objetivo de hoy es: %s.
            Usa la herramienta 'getUserAssessment' para verificar lesiones y nivel.
            Usa 'searchExercises' para elegir movimientos del catálogo.
            Devuelve un JSON estructurado con: nombre de sesión, lista de ejercicios, series, reps y RPE.
            """.formatted(userId, goal);

        ChatResponse response = chatClient.prompt(new Prompt(prompt)).call().chatResponse();

        // 2. Aquí procesaríamos el JSON de respuesta (usando un BeanOutputConverter)
        // Por ahora, simulamos la lógica de guardado en tus tablas relacionales:
        saveToDatabase(userId, planId, response.getResult().getOutput().getContent());

        return "Build completada y guardada en el sistema.";
    }

    @Transactional
    private void saveToDatabase(Long userId, Long planId, String aiJson) {
        // Buscamos el plan activo del usuario
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

        // Creamos la sesión física en la DB
        TrainingSession session = new TrainingSession();
        session.setTrainingPlan(plan);
        session.setName("Generada por Kineo AI");
        session.setScheduledDate(LocalDate.now());

        sessionRepository.save(session);
        log.info("Misión guardada en el loot de la DB.");
    }
}
