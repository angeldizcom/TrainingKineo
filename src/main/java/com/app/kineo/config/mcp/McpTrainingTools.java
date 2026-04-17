package com.app.kineo.config.mcp;

import com.app.kineo.model.Exercise;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.model.UserAssessment;
import com.app.kineo.repository.ExerciseRepository;
import com.app.kineo.repository.TrainingSessionRepository;
import com.app.kineo.repository.UserAssessmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class McpTrainingTools {

    @Bean
    @Description("Consulta el estado físico actual, nivel y lesiones del usuario")
    public Function<UserRequest, UserAssessment> getUserAssessment(UserAssessmentRepository repository) {
        return request -> repository.findByUserId(request.userId())
                .orElseThrow(() -> new RuntimeException("Usuario sin assessment"));
    }

    @Bean
    @Description("Obtiene las últimas sesiones de entrenamiento para analizar el volumen y carga")
    public Function<UserRequest, List<TrainingSession>> getRecentSessions(TrainingSessionRepository repository) {
        return request -> repository.findByTrainingPlanUserId(request.userId());
    }

    @Bean
    @Description("Busca ejercicios en el catálogo filtrando por grupo muscular o equipo")
    public Function<ExerciseFilterRequest, List<Exercise>> searchExercises(ExerciseRepository repository) {
        return filter -> repository.findByMuscleGroupAndEquipment(filter.muscle(), filter.equipment());
    }
}

// DTOs para la comunicación IA <-> Backend
record UserRequest(Long userId) {}
record ExerciseFilterRequest(String muscle, String equipment) {}
