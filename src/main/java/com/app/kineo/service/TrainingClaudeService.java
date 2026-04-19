package com.app.kineo.service;

import com.app.kineo.repository.TrainingSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TrainingClaudeService {

    private final ChatClient claudeClient;

    // Spring AI permite inyectar diferentes ChatModels
    public TrainingClaudeService(AnthropicChatModel chatModel,
                                 TrainingSessionRepository sessionRepository) {
        this.claudeClient = ChatClient.builder(chatModel)
                // ¡IMPORTANTE! Claude también puede usar tus herramientas de MCP
                .defaultFunctions("getUserAssessment", "searchExercises")
                .build();
    }

    public String generateWithClaude(Long userId, String goal) {
        var converter = new BeanOutputConverter<>(TrainingAiService.AiSessionResponse.class);

        return claudeClient.prompt()
                .user(u -> u.text("Genera una sesión para el usuario {userId} con objetivo {goal}. " +
                                "Usa las herramientas disponibles para evitar lesiones.")
                        .param("userId", userId)
                        .param("goal", goal))
                .call()
                .entity(converter)
                .sessionName();
    }
}