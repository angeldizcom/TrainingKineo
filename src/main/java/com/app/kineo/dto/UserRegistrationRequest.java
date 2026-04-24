package com.app.kineo.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Payload de registro — añade el campo password respecto a la versión anterior.
 */
@Data
public class UserRegistrationRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres.")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "El username solo puede contener letras, números, guiones y guiones bajos."
    )
    private String username;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El formato del email no es válido.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres.")
    private String password;

    // ── Assessment ───────────────────────────────────────────

    @NotBlank(message = "El objetivo principal es obligatorio.")
    @Pattern(regexp = "LOSE_WEIGHT|GAIN_MUSCLE|IMPROVE_ENDURANCE|MAINTAIN_FITNESS",
             message = "Objetivo no válido.")
    private String primaryGoal;

    @NotBlank(message = "El nivel de fitness es obligatorio.")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "Nivel no válido.")
    private String fitnessLevel;

    @NotNull @Min(1) @Max(7)
    private Integer trainingFrequency;

    @NotNull @Min(15) @Max(180)
    private Integer preferredDurationMinutes;

    @NotNull @Min(14) @Max(100)
    private Integer age;

    @NotNull @DecimalMin("20.0") @DecimalMax("300.0")
    private Double weight;

    @NotNull @DecimalMin("100.0") @DecimalMax("250.0")
    private Double height;

    @NotBlank
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Género no válido.")
    private String gender;

    @Size(max = 500)
    private String injuries;

    @NotBlank
    @Pattern(regexp = "GYM|HOME_DUMBBELLS|BODYWEIGHT_ONLY", message = "Equipamiento no válido.")
    private String equipmentAccess;
}
