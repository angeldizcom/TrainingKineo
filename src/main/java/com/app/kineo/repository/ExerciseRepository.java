package com.app.kineo.repository;

import com.app.kineo.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByName(String name);
    List<Exercise> findByMuscleGroup(String muscleGroup);
    List<Exercise> findByCategory(String category);
}
