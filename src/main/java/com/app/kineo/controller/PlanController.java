package com.app.kineo.controller;

import com.app.kineo.model.TrainingPlan;
import com.app.kineo.repository.TrainingPlanRepository;
import com.app.kineo.security.KineoPrincipal;
import com.app.kineo.service.TrainingManagerService;
import com.app.kineo.validation.ValidProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Validated
@Tag(name = "Training Plans")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final TrainingManagerService trainingManager;
    private final TrainingPlanRepository planRepository;

    @Operation(
            summary     = "Generar plan de entrenamiento completo",
            description = "userId se extrae del JWT. El plan activo anterior queda como COMPLETED."
    )
    @PostMapping("/generate")
    public ResponseEntity<TrainingPlan> generatePlan(
            @AuthenticationPrincipal KineoPrincipal principal,
            @RequestParam(defaultValue = "4") @Min(1) @Max(12) int weeks,
            @RequestParam(defaultValue = "GEMINI") @ValidProvider String provider) {

        return ResponseEntity.ok(
                trainingManager.processPlanGeneration(principal.getUserId(), weeks, provider)
        );
    }

    @Operation(summary = "Listar mis planes")
    @GetMapping
    public ResponseEntity<List<TrainingPlan>> getMyPlans(
            @AuthenticationPrincipal KineoPrincipal principal) {
        return ResponseEntity.ok(planRepository.findByUserId(principal.getUserId()));
    }

    @Operation(summary = "Detalle de un plan")
    @GetMapping("/{planId}")
    public ResponseEntity<TrainingPlan> getPlan(
            @PathVariable @Positive Long planId) {
        return planRepository.findById(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Generar sesión suelta sobre un plan existente")
    @PostMapping("/sessions/generate")
    public ResponseEntity<String> generateSession(
            @AuthenticationPrincipal KineoPrincipal principal,
            @RequestParam @NotNull @Positive Long planId,
            @RequestParam @NotBlank @Size(max = 200) String goal,
            @RequestParam(defaultValue = "GEMINI") @ValidProvider String provider) {

        return ResponseEntity.ok(
                trainingManager.processSessionBuild(principal.getUserId(), planId, goal, provider)
        );
    }
}