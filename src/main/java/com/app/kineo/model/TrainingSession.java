package com.app.kineo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "training_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TrainingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id")
    private TrainingPlan trainingPlan;

    @Column(nullable = false)
    private String name; // e.g., "Leg Day", "Push A"

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @OneToMany(mappedBy = "trainingSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionExercise> exercises;

    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // Only set when user marks session as done
}
