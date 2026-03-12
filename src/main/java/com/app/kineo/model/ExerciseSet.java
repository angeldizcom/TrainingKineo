package com.app.kineo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_sets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ExerciseSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_exercise_id")
    private SessionExercise sessionExercise;

    @Column(nullable = false)
    private Integer setNumber; // e.g., 1, 2, 3...

    @Column(name = "target_reps")
    private Integer targetReps;

    @Column(name = "actual_reps")
    private Integer actualReps;

    @Column(name = "weight")
    private Double weight; // kg or lbs

    @Column(name = "rpe")
    private Integer rpe; // Rate of Perceived Exertion (1-10)

    private Boolean completed; // Whether the set was finished successfully

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
