package com.app.kineo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    // --- Objetivos y Preferencias ---
    @Column(name = "primary_goal")
    private String primaryGoal; // e.g., LOSE_WEIGHT, GAIN_MUSCLE, IMPROVE_ENDURANCE

    @Column(name = "fitness_level")
    private String fitnessLevel; // e.g., BEGINNER, INTERMEDIATE, ADVANCED

    @Column(name = "training_frequency")
    private Integer trainingFrequency; // Days per week available

    @Column(name = "preferred_duration")
    private Integer preferredDurationMinutes; // 30, 45, 60, 90 mins

    // --- Datos Físicos Básicos ---
    private Integer age;
    private Double weight; // kg
    private Double height; // cm
    private String gender;

    // --- Limitaciones ---
    @Column(name = "injuries")
    private String injuries; // Text describing any injuries or limitations

    @Column(name = "equipment_access")
    private String equipmentAccess; // GYM, HOME_DUMBBELLS, BODYWEIGHT_ONLY

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime takenAt;
}
