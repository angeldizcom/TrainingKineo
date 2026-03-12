package com.app.kineo.service;

import com.app.kineo.dto.AiTrainingPlanDto;
import com.app.kineo.model.*;
import com.app.kineo.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingPlanService {

    private final ChatClient chatClient;
    private final TrainingPlanRepository planRepository;
    private final TrainingSessionRepository sessionRepository;
    private final ExerciseRepository exerciseRepository;
    private final SessionExerciseRepository sessionExerciseRepository;
    private final ObjectMapper objectMapper;

    public TrainingPlanService(ChatClient.Builder chatClientBuilder,
                               TrainingPlanRepository planRepository,
                               TrainingSessionRepository sessionRepository,
                               ExerciseRepository exerciseRepository,
                               SessionExerciseRepository sessionExerciseRepository,
                               ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
        this.exerciseRepository = exerciseRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TrainingPlan generatePlanForUser(User user) {
        UserAssessment assessment = user.getCurrentAssessment();
        if (assessment == null) {
            throw new IllegalStateException("User does not have an assessment. Cannot generate plan.");
        }

        String prompt = constructPrompt(assessment);
        String jsonResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        jsonResponse = cleanJson(jsonResponse);

        try {
            AiTrainingPlanDto aiPlan = objectMapper.readValue(jsonResponse, AiTrainingPlanDto.class);
            return savePlanFromAi(user, aiPlan);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }

    private String cleanJson(String response) {
        if (response.startsWith("```json")) {
            return response.substring(7, response.lastIndexOf("```")).trim();
        } else if (response.startsWith("```")) {
            return response.substring(3, response.lastIndexOf("```")).trim();
        }
        return response.trim();
    }

    private TrainingPlan savePlanFromAi(User user, AiTrainingPlanDto aiPlan) {
        TrainingPlan plan = new TrainingPlan();
        plan.setUser(user);
        plan.setName(aiPlan.getName());
        plan.setGoal(aiPlan.getGoal());
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusWeeks(4));
        plan.setStatus("ACTIVE");

        plan = planRepository.save(plan);

        int dayOffset = 0;
        if (aiPlan.getSessions() != null) {
            for (AiTrainingPlanDto.AiSessionDto sessionDto : aiPlan.getSessions()) {
                TrainingSession session = new TrainingSession();
                session.setTrainingPlan(plan);
                session.setName(sessionDto.getDayName());
                session.setNotes(sessionDto.getFocus());
                session.setScheduledDate(plan.getStartDate().plusDays(dayOffset));
                dayOffset++;
                
                session = sessionRepository.save(session);

                int order = 1;
                if (sessionDto.getExercises() != null) {
                    for (AiTrainingPlanDto.AiExerciseDto exDto : sessionDto.getExercises()) {
                        Exercise exercise = findOrCreateExercise(exDto);

                        SessionExercise sessionExercise = new SessionExercise();
                        sessionExercise.setTrainingSession(session);
                        sessionExercise.setExercise(exercise);
                        sessionExercise.setOrderIndex(order++);
                        sessionExercise.setNotes(exDto.getNotes());

                        sessionExerciseRepository.save(sessionExercise);
                    }
                }
            }
        }
        return plan;
    }

    private Exercise findOrCreateExercise(AiTrainingPlanDto.AiExerciseDto exDto) {
        Optional<Exercise> existing = exerciseRepository.findByName(exDto.getName());
        if (existing.isPresent()) {
            return existing.get();
        }
        Exercise newEx = new Exercise();
        newEx.setName(exDto.getName());
        newEx.setMuscleGroup(exDto.getMuscleTarget());
        newEx.setDescription("AI Generated Exercise");
        newEx.setCategory("GENERAL"); 
        return exerciseRepository.save(newEx);
    }

    private String constructPrompt(UserAssessment assessment) {
        return String.format("""
                Act as an expert personal trainer. Create a 1-week training plan for a client with the following profile:
                - Goal: %s
                - Level: %s
                - Days available: %d
                - Duration per session: %d mins
                - Equipment: %s
                - Injuries/Limitations: %s
                - Gender: %s, Age: %d, Weight: %.1fkg, Height: %.1fcm

                Return the plan ONLY as valid JSON matching this structure:
                {
                  "name": "Plan Name",
                  "goal": "Goal description",
                  "description": "Brief overview",
                  "sessions": [
                    {
                      "dayName": "Day 1: Legs",
                      "focus": "Strength",
                      "exercises": [
                        {
                          "name": "Squat",
                          "muscleTarget": "Legs",
                          "sets": 3,
                          "reps": 10,
                          "notes": "Rest 90s"
                        }
                      ]
                    }
                  ]
                }
                DO NOT add any markdown formatting or text outside the JSON.
                """,
                assessment.getPrimaryGoal(),
                assessment.getFitnessLevel(),
                assessment.getTrainingFrequency(),
                assessment.getPreferredDurationMinutes(),
                assessment.getEquipmentAccess(),
                assessment.getInjuries(),
                assessment.getGender(), assessment.getAge(), assessment.getWeight(), assessment.getHeight()
        );
    }
}
