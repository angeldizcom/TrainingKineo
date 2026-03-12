package com.app.kineo.repository;

import com.app.kineo.model.ExerciseSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseSetRepository extends JpaRepository<ExerciseSet, Long> {
    List<ExerciseSet> findBySessionExerciseIdOrderBySetNumberAsc(Long sessionExerciseId);
}
