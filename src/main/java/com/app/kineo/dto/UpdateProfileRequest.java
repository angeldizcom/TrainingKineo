package com.app.kineo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres.")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "El username solo puede contener letras, números, guiones y guiones bajos."
    )
    private String username; // null = no cambiar

    @Email(message = "Formato de email inválido.")
    @Size(max = 255)
    private String email; // null = no cambiar

    /** Contraseña actual — requerida para cambiar email o contraseña. */
    private String currentPassword;

    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres.")
    private String newPassword; // null = no cambiar
}
