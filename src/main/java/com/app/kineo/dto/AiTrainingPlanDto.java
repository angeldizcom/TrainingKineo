package com.app.kineo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class AiTrainingPlanDto {
    private String name;
    private String goal;
    private String description;
    private List<AiSessionDto> sessions;

    @Data
    @NoArgsConstructor
    public static class AiSessionDto {
        private String dayName;
        private String focus;
        private List<AiExerciseDto> exercises;
    }

    @Data
    @NoArgsConstructor
    public static class AiExerciseDto {
        private String name;
        private String muscleTarget;
        private int sets;
        private int reps;
        private String notes;
    }
}
