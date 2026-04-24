package com.app.kineo.controller;

import com.app.kineo.model.TrainingAdvice;
import com.app.kineo.service.TrainingManagerService;
import com.app.kineo.service.TrainingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;
    private final TrainingManagerService trainingManager;


    public TrainingController(TrainingService trainingService, TrainingManagerService trainingManager) {
        this.trainingService = trainingService;
        this.trainingManager = trainingManager;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generate(
            @RequestParam Long userId,
            @RequestParam Long planId,
            @RequestParam String goal,
            @RequestParam(defaultValue = "GEMINI") String provider) {

        // Dispara la IA (Gemini/Claude) + Guarda en DB
        String message = trainingManager.processSessionBuild(userId, planId, goal, provider);
        return ResponseEntity.ok(message);
    }
    @GetMapping("/history")
    public List<TrainingAdvice> getAllAdvices() {
        return trainingService.getAllAdvices();
    }

    @GetMapping("/search")
    public List<TrainingAdvice> searchAdvices(@RequestParam String topic) {
        return trainingService.searchAdvices(topic);
    }
}
