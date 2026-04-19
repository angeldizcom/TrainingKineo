package com.app.kineo.service;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.stereotype.Service;

@Service
public class TrainingManagerService {

    private final GoogleGenAiChatModel geminiModel;
    private final AnthropicChatModel anthropicModel;
    private final TrainingAiService trainingAiService;

    public TrainingManagerService(GoogleGenAiChatModel geminiModel,
                                  AnthropicChatModel anthropicModel,
                                  TrainingAiService trainingAiService) {
        this.geminiModel = geminiModel;
        this.anthropicModel = anthropicModel;
        this.trainingAiService = trainingAiService;
    }

    public String processBuild(Long userId, Long planId, String goal, String provider) {
        // Elegimos el cliente según el "provider" que venga de Android
        var selectedModel = provider.equalsIgnoreCase("CLAUDE") ? anthropicModel : geminiModel;

        // Construcción del cliente con soporte para tus herramientas MCP
        ChatClient client = ChatClient.builder(selectedModel)
                .defaultFunctions("getUserAssessment", "searchExercises")
                .build();

        var converter = new BeanOutputConverter<>(TrainingAiService.AiSessionResponse.class);

        // Prompt estructurado para el loot de entrenamiento
        TrainingAiService.AiSessionResponse response = client.prompt()
                .user(u -> u.text("Genera sesión para {userId}. Objetivo: {goal}. Formato: {format}")
                        .param("userId", userId)
                        .param("goal", goal)
                        .param("format", converter.getFormat()))
                .call()
                .entity(converter);

        trainingAiService.saveToDatabase(planId, response);
        return "Build finalizada con " + provider + ": " + response.sessionName();
    }
}
