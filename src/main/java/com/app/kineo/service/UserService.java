package com.app.kineo.service;

import com.app.kineo.dto.UserRegistrationRequest;
import com.app.kineo.model.User;
import com.app.kineo.model.UserAssessment;
import com.app.kineo.repository.UserAssessmentRepository;
import com.app.kineo.repository.UserRepository;
import com.app.kineo.service.TrainingPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserAssessmentRepository assessmentRepository;
    private final TrainingPlanService trainingPlanService;

    public UserService(UserRepository userRepository, UserAssessmentRepository assessmentRepository, TrainingPlanService trainingPlanService) {
        this.userRepository = userRepository;
        this.assessmentRepository = assessmentRepository;
        this.trainingPlanService = trainingPlanService;
    }

    @Transactional
    public User registerUserAndGeneratePlan(UserRegistrationRequest request) {
        // 1. Create User
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user = userRepository.save(user);

        // 2. Create Assessment
        UserAssessment assessment = new UserAssessment();
        assessment.setUser(user);
        assessment.setPrimaryGoal(request.getPrimaryGoal());
        assessment.setFitnessLevel(request.getFitnessLevel());
        assessment.setTrainingFrequency(request.getTrainingFrequency());
        assessment.setPreferredDurationMinutes(request.getPreferredDurationMinutes());
        assessment.setAge(request.getAge());
        assessment.setWeight(request.getWeight());
        assessment.setHeight(request.getHeight());
        assessment.setGender(request.getGender());
        assessment.setInjuries(request.getInjuries());
        assessment.setEquipmentAccess(request.getEquipmentAccess());
        
        assessmentRepository.save(assessment);
        
        // Update user reference
        user.setCurrentAssessment(assessment);

        // 3. Trigger AI Plan Generation
        trainingPlanService.generatePlanForUser(user);

        return user;
    }
}
