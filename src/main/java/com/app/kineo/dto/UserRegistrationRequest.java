package com.app.kineo.dto;

import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String username;
    private String email;
    
    // Assessment Data
    private String primaryGoal;
    private String fitnessLevel;
    private Integer trainingFrequency;
    private Integer preferredDurationMinutes;
    private Integer age;
    private Double weight;
    private Double height;
    private String gender;
    private String injuries;
    private String equipmentAccess;
}
