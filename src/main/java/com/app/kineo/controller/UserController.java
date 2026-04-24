package com.app.kineo.controller;

import com.app.kineo.dto.UpdateAssessmentRequest;
import com.app.kineo.dto.UpdateProfileRequest;
import com.app.kineo.dto.UserProfileDto;
import com.app.kineo.dto.UserRegistrationRequest;
import com.app.kineo.security.KineoPrincipal;
import com.app.kineo.service.UserManagementService;
import com.app.kineo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "Registro y gestión de perfil de usuario")
public class UserController {

    private final UserService           userService;
    private final UserManagementService userManagementService;

    // ─────────────────────────────────────────────────────────
    // Público — sin JWT
    // ─────────────────────────────────────────────────────────

    @Operation(
        summary     = "Registrar usuario",
        description = "Crea el usuario, guarda el assessment y genera el primer plan con IA."
    )
    @PostMapping("/register")
    public ResponseEntity<UserProfileDto> registerUser(
            @RequestBody @Valid UserRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserProfileDto.from(userService.registerUserAndGeneratePlan(request)));
    }

    // ─────────────────────────────────────────────────────────
    // Protegidos — requieren JWT
    // ─────────────────────────────────────────────────────────

    @Operation(summary = "Ver mi perfil")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(
            @AuthenticationPrincipal KineoPrincipal principal) {
        return ResponseEntity.ok(userManagementService.getProfile(principal.getUserId()));
    }

    @Operation(
        summary     = "Actualizar datos de cuenta",
        description = "Permite cambiar username, email y/o contraseña. " +
                      "Cambiar email o contraseña requiere enviar `currentPassword`."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal KineoPrincipal principal,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(
                userManagementService.updateProfile(principal.getUserId(), request)
        );
    }

    @Operation(
        summary     = "Actualizar assessment",
        description = "Actualiza el perfil físico y objetivos del usuario. " +
                      "Si `regeneratePlan=true`, genera un nuevo plan de entrenamiento " +
                      "adaptado al assessment modificado e invalida el anterior."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me/assessment")
    public ResponseEntity<UserProfileDto> updateAssessment(
            @AuthenticationPrincipal KineoPrincipal principal,
            @RequestBody @Valid UpdateAssessmentRequest request) {
        return ResponseEntity.ok(
                userManagementService.updateAssessment(principal.getUserId(), request)
        );
    }

    @Operation(
        summary     = "Eliminar cuenta",
        description = "Elimina la cuenta y todos los datos del usuario (planes, sesiones, series). " +
                      "Requiere contraseña actual como confirmación. Acción irreversible."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal KineoPrincipal principal,
            @RequestParam @NotBlank(message = "La contraseña es requerida para eliminar la cuenta.")
            String password) {
        userManagementService.deleteAccount(principal.getUserId(), password);
        return ResponseEntity.noContent().build();
    }
}
