package com.app.kineo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class TrainingAiService {

    private final ChatClient chatClient;

    public TrainingAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generateTrainingAdvice(String topic) {
        return chatClient.prompt()
                .user("Genera un consejo breve de entrenamiento sobre: " + topic)
                .call()
                .content();
    }
}
