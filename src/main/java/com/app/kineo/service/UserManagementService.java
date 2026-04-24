package com.app.kineo.service;

import com.app.kineo.dto.UpdateAssessmentRequest;
import com.app.kineo.dto.UpdateProfileRequest;
import com.app.kineo.dto.UserProfileDto;
import com.app.kineo.exception.UserAlreadyExistsException;
import com.app.kineo.exception.UserNotFoundException;
import com.app.kineo.model.User;
import com.app.kineo.model.UserAssessment;
import com.app.kineo.repository.UserAssessmentRepository;
import com.app.kineo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository        userRepository;
    private final UserAssessmentRepository assessmentRepository;
    private final TrainingPlanService   trainingPlanService;
    private final PasswordEncoder       passwordEncoder;

    // ─────────────────────────────────────────────────────────
    // Consulta
    // ─────────────────────────────────────────────────────────

    /**
     * Devuelve el perfil completo del usuario autenticado.
     * El passwordHash nunca sale del service — UserProfileDto lo omite.
     */
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = findOrThrow(userId);
        return UserProfileDto.from(user);
    }

    // ─────────────────────────────────────────────────────────
    // Actualizar perfil de cuenta
    // ─────────────────────────────────────────────────────────

    /**
     * Actualiza username, email y/o contraseña.
     *
     * Reglas:
     * · Cambiar email o contraseña requiere enviar la contraseña actual.
     * · El username puede cambiarse sin contraseña actual.
     * · Cada campo es opcional — solo se actualiza si viene en el request.
     */
    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findOrThrow(userId);

        // Username — verificar unicidad si cambia
        if (request.getUsername() != null
                && !request.getUsername().equals(user.getUsername())) {
            userRepository.findByUsername(request.getUsername()).ifPresent(u -> {
                throw new UserAlreadyExistsException("username", request.getUsername());
            });
            user.setUsername(request.getUsername());
            log.info("[UserManagement] Username actualizado para userId={}.", userId);
        }

        // Email — requiere contraseña actual
        if (request.getEmail() != null
                && !request.getEmail().equals(user.getEmail())) {
            verifyCurrentPassword(user, request.getCurrentPassword());
            userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
                throw new UserAlreadyExistsException("email", request.getEmail());
            });
            user.setEmail(request.getEmail());
            log.info("[UserManagement] Email actualizado para userId={}.", userId);
        }

        // Contraseña — requiere contraseña actual
        if (request.getNewPassword() != null) {
            verifyCurrentPassword(user, request.getCurrentPassword());
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            log.info("[UserManagement] Contraseña actualizada para userId={}.", userId);
        }

        userRepository.save(user);
        return UserProfileDto.from(user);
    }

    // ─────────────────────────────────────────────────────────
    // Actualizar assessment
    // ─────────────────────────────────────────────────────────

    /**
     * Actualiza el assessment del usuario.
     *
     * Si {@code request.isRegeneratePlan()} es true, desactiva el plan
     * activo actual y genera uno nuevo adaptado al assessment modificado.
     * Útil cuando el usuario cambia de objetivo (ej: de LOSE_WEIGHT a GAIN_MUSCLE).
     */
    @Transactional
    public UserProfileDto updateAssessment(Long userId, UpdateAssessmentRequest request) {
        User user = findOrThrow(userId);

        UserAssessment assessment = user.getCurrentAssessment();
        if (assessment == null) {
            // Si no tenía assessment, lo creamos
            assessment = new UserAssessment();
            assessment.setUser(user);
        }

        // Solo actualizamos los campos que vienen en el request (null = no cambiar)
        if (request.getPrimaryGoal()            != null) assessment.setPrimaryGoal(request.getPrimaryGoal());
        if (request.getFitnessLevel()           != null) assessment.setFitnessLevel(request.getFitnessLevel());
        if (request.getTrainingFrequency()      != null) assessment.setTrainingFrequency(request.getTrainingFrequency());
        if (request.getPreferredDurationMinutes() != null) assessment.setPreferredDurationMinutes(request.getPreferredDurationMinutes());
        if (request.getAge()                    != null) assessment.setAge(request.getAge());
        if (request.getWeight()                 != null) assessment.setWeight(request.getWeight());
        if (request.getHeight()                 != null) assessment.setHeight(request.getHeight());
        if (request.getGender()                 != null) assessment.setGender(request.getGender());
        if (request.getInjuries()               != null) assessment.setInjuries(request.getInjuries());
        if (request.getEquipmentAccess()        != null) assessment.setEquipmentAccess(request.getEquipmentAccess());

        assessmentRepository.save(assessment);
        user.setCurrentAssessment(assessment);

        log.info("[UserManagement] Assessment actualizado para userId={}.", userId);

        // Regenerar plan si el usuario lo solicita explícitamente
        if (request.isRegeneratePlan()) {
            log.info("[UserManagement] Regenerando plan para userId={} tras cambio de assessment.", userId);
            trainingPlanService.generatePlanForUser(user);
        }

        return UserProfileDto.from(user);
    }

    // ─────────────────────────────────────────────────────────
    // Eliminar cuenta
    // ─────────────────────────────────────────────────────────

    /**
     * Elimina la cuenta del usuario y todos sus datos relacionados.
     * El CascadeType.ALL en User → assessment, planes, sesiones, sets
     * se encarga de la eliminación en cascada desde la base de datos.
     *
     * Requiere contraseña actual como confirmación.
     */
    @Transactional
    public void deleteAccount(Long userId, String currentPassword) {
        User user = findOrThrow(userId);
        verifyCurrentPassword(user, currentPassword);
        userRepository.delete(user);
        log.info("[UserManagement] Cuenta eliminada para userId={}.", userId);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /**
     * Verifica que la contraseña actual sea correcta.
     * Lanza 401 si es incorrecta o si el usuario no tiene contraseña asignada.
     */
    private void verifyCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            // Usamos un mensaje genérico intencionadamente — no revelamos si
            // el problema es "no tiene contraseña" o "contraseña incorrecta"
            throw new com.app.kineo.exception.InvalidCredentialsException();
        }
    }
}
