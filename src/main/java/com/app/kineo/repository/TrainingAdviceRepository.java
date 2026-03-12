package com.app.kineo.repository;

import com.app.kineo.model.TrainingAdvice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingAdviceRepository extends JpaRepository<TrainingAdvice, Long> {
    List<TrainingAdvice> findByTopicContainingIgnoreCase(String topic);
}
