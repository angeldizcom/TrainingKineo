// ─────────────────────────────────────────────────────────────────────
// AuthController.java — login con verificación BCrypt real
// ────────────────────────────────────────────────────────────
package com.app.kineo.service;

import com.app.kineo.dto.UserRegistrationRequest;
import com.app.kineo.exception.UserAlreadyExistsException;
import com.app.kineo.model.User;
import com.app.kineo.model.UserAssessment;
import com.app.kineo.repository.UserAssessmentRepository;
import com.app.kineo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository           userRepository;
    private final UserAssessmentRepository assessmentRepository;
    private final TrainingPlanService      trainingPlanService;
    private final PasswordEncoder          passwordEncoder;

    @Transactional
    public User registerUserAndGeneratePlan(UserRegistrationRequest request) {
        // Verificar unicidad antes de crear
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new UserAlreadyExistsException("email", request.getEmail());
        });
        userRepository.findByUsername(request.getUsername()).ifPresent(u -> {
            throw new UserAlreadyExistsException("username", request.getUsername());
        });

        // Crear usuario con contraseña hasheada
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        // Crear assessment
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

        user.setCurrentAssessment(assessment);

        // Generar plan inicial con IA
        trainingPlanService.generatePlanForUser(user);

        return user;
    }
}
