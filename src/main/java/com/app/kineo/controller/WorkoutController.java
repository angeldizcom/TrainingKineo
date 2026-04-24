package com.app.kineo.controller;

import com.app.kineo.dto.CompleteSetRequest;
import com.app.kineo.dto.SessionDetailDto;
import com.app.kineo.security.KineoPrincipal;
import com.app.kineo.service.TrainingManagerService;
import com.app.kineo.service.WorkoutExecutionService;
import com.app.kineo.validation.ValidProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workout")
@RequiredArgsConstructor
@Validated
@Tag(name = "Workout Execution")
@SecurityRequirement(name = "bearerAuth") // Swagger mostrará el candado en estos endpoints
public class WorkoutController {

    private final WorkoutExecutionService workoutService;
    private final TrainingManagerService  trainingManager;

    @Operation(summary = "Obtener detalle de sesión")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionDetailDto> getSession(
            @PathVariable @Positive Long sessionId) {
        return ResponseEntity.ok(workoutService.getSessionDetail(sessionId));
    }

    @Operation(summary = "Completar una serie")
    @PatchMapping("/sessions/{sessionId}/sets/{setId}/complete")
    public ResponseEntity<SessionDetailDto> completeSet(
            @PathVariable @Positive Long sessionId,
            @PathVariable @Positive Long setId,
            @RequestBody @Valid CompleteSetRequest request) {
        return ResponseEntity.ok(workoutService.completeSet(sessionId, setId, request));
    }

    @Operation(summary = "Cerrar sesión de entrenamiento")
    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<SessionDetailDto> completeSession(
            @PathVariable @Positive Long sessionId) {
        return ResponseEntity.ok(workoutService.completeSession(sessionId));
    }

    @Operation(
            summary     = "Generar siguiente sesión adaptativa",
            description = "userId se extrae del JWT — no hace falta enviarlo como parámetro."
    )
    @PostMapping("/sessions/adaptive")
    public ResponseEntity<String> generateAdaptiveSession(
            @AuthenticationPrincipal KineoPrincipal principal, // ← del token
            @RequestParam @NotNull @Positive Long planId,
            @RequestParam(required = false) String goal,
            @RequestParam(defaultValue = "CLAUDE") @ValidProvider String provider) {

        String sessionName = trainingManager.processAdaptiveSession(
                principal.getUserId(), planId, goal, provider
        );
        return ResponseEntity.ok("Sesión '" + sessionName + "' generada con " + provider + ".");
    }
}