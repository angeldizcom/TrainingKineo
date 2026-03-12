package com.app.kineo.repository;

import com.app.kineo.model.TrainingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {
    List<TrainingPlan> findByUserId(Long userId);
    Optional<TrainingPlan> findByUserIdAndStatus(Long userId, String status); // e.g., ACTIVE
    List<TrainingPlan> findByUserIdAndStartDateAfter(Long userId, LocalDate date);
}
