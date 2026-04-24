package com.app.kineo.dto;

import com.app.kineo.model.User;
import com.app.kineo.model.UserAssessment;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────────────────────
// RESPONSE — perfil completo del usuario (sin passwordHash)
// ─────────────────────────────────────────────────────────────────────

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDto {

    private Long          id;
    private String        username;
    private String        email;
    private LocalDateTime joinedAt;

    // Assessment inline — null si el usuario aún no lo completó
    private AssessmentDto assessment;

    @Getter
    @Builder
    public static class AssessmentDto {
        private String  primaryGoal;
        private String  fitnessLevel;
        private Integer trainingFrequency;
        private Integer preferredDurationMinutes;
        private Integer age;
        private Double  weight;
        private Double  height;
        private String  gender;
        private String  injuries;
        private String  equipmentAccess;
    }

    // ── Factory ──────────────────────────────────────────────

    public static UserProfileDto from(User user) {
        AssessmentDto assessmentDto = null;
        if (user.getCurrentAssessment() != null) {
            UserAssessment a = user.getCurrentAssessment();
            assessmentDto = AssessmentDto.builder()
                    .primaryGoal(a.getPrimaryGoal())
                    .fitnessLevel(a.getFitnessLevel())
                    .trainingFrequency(a.getTrainingFrequency())
                    .preferredDurationMinutes(a.getPreferredDurationMinutes())
                    .age(a.getAge())
                    .weight(a.getWeight())
                    .height(a.getHeight())
                    .gender(a.getGender())
                    .injuries(a.getInjuries())
                    .equipmentAccess(a.getEquipmentAccess())
                    .build();
        }

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .joinedAt(user.getJoinedAt())
                .assessment(assessmentDto)
                .build();
    }
}


// ─────────────────────────────────────────────────────────────────────
// REQUEST — actualizar datos de cuenta (username / email / password)
// ─────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────
// REQUEST — actualizar assessment (puede disparar nuevo plan)
// ─────────────────────────────────────────────────────────────────────

