package com.app.kineo.repository;

import com.app.kineo.model.TrainingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    List<TrainingSession> findByTrainingPlanId(Long trainingPlanId);
    Optional<TrainingSession> findByTrainingPlanIdAndScheduledDate(Long trainingPlanId, LocalDate date);
    List<TrainingSession> findByTrainingPlanIdAndCompletedAtNotNull(Long trainingPlanId); // Find completed sessions
}
