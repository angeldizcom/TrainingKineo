package com.app.kineo.controller;

import com.app.kineo.model.TrainingAdvice;
import com.app.kineo.service.TrainingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/generate")
    public TrainingAdvice generateAndSaveAdvice(@RequestParam String topic) {
        return trainingService.getAndSaveAdvice(topic);
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
