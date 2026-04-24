package com.app.kineo.controller;

import com.app.kineo.dto.AdherenceDto;
import com.app.kineo.dto.ExerciseProgressDto;
import com.app.kineo.dto.WeeklyVolumeDto;
import com.app.kineo.security.KineoPrincipal;
import com.app.kineo.service.ProgressAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
@Validated
@Tag(name = "Progress & Analytics")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressAnalyticsService analyticsService;

    @Operation(
            summary     = "Progresión de carga de un ejercicio",
            description = "Historial de peso × reps del usuario autenticado para un ejercicio. " +
                    "userId se extrae del JWT."
    )
    @GetMapping("/exercises/{exerciseId}")
    public ResponseEntity<ExerciseProgressDto> getExerciseProgress(
            @AuthenticationPrincipal KineoPrincipal principal,
            @PathVariable @Positive Long exerciseId) {
        return ResponseEntity.ok(
                analyticsService.getExerciseProgress(principal.getUserId(), exerciseId)
        );
    }

    @Operation(
            summary     = "Volumen semanal por grupo muscular",
            description = "Tonelaje de las últimas N semanas del usuario autenticado."
    )
    @GetMapping("/volume")
    public ResponseEntity<WeeklyVolumeDto> getWeeklyVolume(
            @AuthenticationPrincipal KineoPrincipal principal,
            @RequestParam(defaultValue = "4") @Min(1) @Max(52) int weeks) {
        return ResponseEntity.ok(
                analyticsService.getWeeklyVolume(principal.getUserId(), weeks)
        );
    }

    @Operation(summary = "Adherencia al plan de entrenamiento")
    @GetMapping("/plans/{planId}/adherence")
    public ResponseEntity<AdherenceDto> getPlanAdherence(
            @PathVariable @Positive Long planId) {
        return ResponseEntity.ok(analyticsService.getPlanAdherence(planId));
    }
}