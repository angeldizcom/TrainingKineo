package com.app.kineo.repository;

import com.app.kineo.model.UserAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAssessmentRepository extends JpaRepository<UserAssessment, Long> {
    Optional<UserAssessment> findByUserId(Long userId);
}
