package com.app.kineo.repository;

import com.app.kineo.model.SessionExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, Long> {
    List<SessionExercise> findByTrainingSessionIdOrderByOrderIndexAsc(Long trainingSessionId);
}
