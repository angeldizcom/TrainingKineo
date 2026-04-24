package com.app.kineo.service;

import com.app.kineo.dto.AdherenceDto;
import com.app.kineo.dto.ExerciseProgressDto;
import com.app.kineo.dto.ExerciseProgressDto.ProgressPoint;
import com.app.kineo.dto.WeeklyVolumeDto;
import com.app.kineo.dto.WeeklyVolumeDto.MuscleVolume;
import com.app.kineo.dto.WeeklyVolumeDto.WeekSummary;
import com.app.kineo.exception.ExerciseNotFoundException;
import com.app.kineo.exception.PlanNotFoundException;
import com.app.kineo.model.ExerciseSet;
import com.app.kineo.model.TrainingSession;
import com.app.kineo.repository.ExerciseRepository;
import com.app.kineo.repository.ExerciseSetRepository;
import com.app.kineo.repository.TrainingPlanRepository;
import com.app.kineo.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressAnalyticsService {

    private final ExerciseSetRepository     setRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingPlanRepository    planRepository;
    private final ExerciseRepository        exerciseRepository;

    @Transactional(readOnly = true)
    public ExerciseProgressDto getExerciseProgress(Long userId, Long exerciseId) {
        var exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));

        List<ExerciseSet> completedSets =
                setRepository.findCompletedSetsByUserAndExercise(userId, exerciseId);

        Map<LocalDate, ExerciseSet> bestPerDay = completedSets.stream()
                .collect(Collectors.toMap(
                        es -> es.getSessionExercise().getTrainingSession().getScheduledDate(),
                        es -> es,
                        (a, b) -> volume(a) >= volume(b) ? a : b,
                        LinkedHashMap::new
                ));

        List<ProgressPoint> history = bestPerDay.entrySet().stream()
                .map(e -> toProgressPoint(e.getKey(), e.getValue()))
                .toList();

        ProgressPoint personalBest = history.stream()
                .max(Comparator.comparingDouble(p -> p.getVolume() != null ? p.getVolume() : 0))
                .orElse(null);

        return ExerciseProgressDto.builder()
                .exerciseId(exercise.getId())
                .exerciseName(exercise.getName())
                .muscleGroup(exercise.getMuscleGroup())
                .history(history)
                .personalBest(personalBest)
                .build();
    }

    @Transactional(readOnly = true)
    public WeeklyVolumeDto getWeeklyVolume(Long userId, int weeks) {
        LocalDate since = LocalDate.now().minusWeeks(weeks);
        List<ExerciseSet> sets = setRepository.findCompletedSetsForVolumeAnalysis(userId, since);

        Map<LocalDate, Map<String, List<ExerciseSet>>> byWeekAndMuscle = sets.stream()
                .collect(Collectors.groupingBy(
                        es -> weekStart(es.getSessionExercise().getTrainingSession().getScheduledDate()),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                es -> es.getSessionExercise().getExercise().getMuscleGroup()
                        )
                ));

        List<WeekSummary> weekSummaries = byWeekAndMuscle.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, Map<String, List<ExerciseSet>>>comparingByKey().reversed())
                .map(weekEntry -> {
                    List<MuscleVolume> muscleVolumes = weekEntry.getValue().entrySet().stream()
                            .map(m -> MuscleVolume.builder()
                                    .muscleGroup(m.getKey())
                                    .volume(m.getValue().stream().mapToDouble(this::volume).sum())
                                    .totalSets(m.getValue().size())
                                    .build())
                            .sorted(Comparator.comparingDouble(MuscleVolume::getVolume).reversed())
                            .toList();

                    double totalVolume = muscleVolumes.stream()
                            .mapToDouble(MuscleVolume::getVolume).sum();

                    long completedSessions = sets.stream()
                            .filter(es -> weekStart(
                                    es.getSessionExercise().getTrainingSession().getScheduledDate())
                                    .equals(weekEntry.getKey()))
                            .map(es -> es.getSessionExercise().getTrainingSession().getId())
                            .distinct().count();

                    return WeekSummary.builder()
                            .weekStart(weekEntry.getKey())
                            .byMuscleGroup(muscleVolumes)
                            .totalVolume(totalVolume)
                            .completedSessions((int) completedSessions)
                            .build();
                })
                .toList();

        return WeeklyVolumeDto.builder().weeks(weekSummaries).build();
    }

    @Transactional(readOnly = true)
    public AdherenceDto getPlanAdherence(Long planId) {
        planRepository.findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));

        List<TrainingSession> sessions =
                sessionRepository.findByPlanIdWithExercisesAndSets(planId);

        int total     = sessions.size();
        int completed = (int) sessions.stream().filter(s -> s.getCompletedAt() != null).count();

        return AdherenceDto.builder()
                .planId(planId)
                .planName(sessions.isEmpty() ? "" : sessions.get(0).getTrainingPlan().getName())
                .totalSessions(total)
                .completedSessions(completed)
                .pendingSessions(total - completed)
                .adherencePercent(total == 0 ? 0 : completed * 100 / total)
                .sessions(sessions.stream().map(this::toSessionAdherence).toList())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private double volume(ExerciseSet es) {
        int reps    = es.getActualReps() != null ? es.getActualReps() : 0;
        double w    = es.getWeight()     != null ? es.getWeight()     : 1.0;
        return reps * w;
    }

    private ProgressPoint toProgressPoint(LocalDate date, ExerciseSet es) {
        var session = es.getSessionExercise().getTrainingSession();
        return ProgressPoint.builder()
                .date(date)
                .sessionId(session.getId())
                .sessionName(session.getName())
                .setNumber(es.getSetNumber())
                .reps(es.getActualReps())
                .weight(es.getWeight())
                .volume(volume(es))
                .rpe(es.getRpe())
                .build();
    }

    private AdherenceDto.SessionAdherence toSessionAdherence(TrainingSession session) {
        int totalSets = session.getExercises() == null ? 0 :
                session.getExercises().stream()
                        .mapToInt(se -> se.getSets() == null ? 0 : se.getSets().size()).sum();

        int doneSets = session.getExercises() == null ? 0 :
                session.getExercises().stream()
                        .filter(se -> se.getSets() != null)
                        .flatMap(se -> se.getSets().stream())
                        .mapToInt(s -> Boolean.TRUE.equals(s.getCompleted()) ? 1 : 0).sum();

        return AdherenceDto.SessionAdherence.builder()
                .sessionId(session.getId())
                .sessionName(session.getName())
                .scheduledDate(session.getScheduledDate())
                .completed(session.getCompletedAt() != null)
                .setsCompletionPercent(totalSets == 0 ? 0 : doneSets * 100 / totalSets)
                .build();
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
}