package com.app.kineo.service;

import com.app.kineo.model.TrainingAdvice;
import com.app.kineo.repository.TrainingAdviceRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrainingService {

    private final ChatClient chatClient;
    private final TrainingAdviceRepository repository;

    public TrainingService(ChatClient.Builder chatClientBuilder, TrainingAdviceRepository repository) {
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
    }

    @Transactional
    public TrainingAdvice getAndSaveAdvice(String topic) {
        String response = chatClient.prompt()
                .user("Genera un consejo breve y profesional de entrenamiento sobre: " + topic)
                .call()
                .content();
        
        TrainingAdvice advice = new TrainingAdvice(topic, response);
        return repository.save(advice);
    }

    public List<TrainingAdvice> getAllAdvices() {
        return repository.findAll();
    }
    
    public List<TrainingAdvice> searchAdvices(String topic) {
        return repository.findByTopicContainingIgnoreCase(topic);
    }
}
