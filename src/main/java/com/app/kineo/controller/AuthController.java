package com.app.kineo.controller;

import com.app.kineo.exception.InvalidCredentialsException;
import com.app.kineo.model.User;
import com.app.kineo.repository.UserRepository;
import com.app.kineo.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticación y emisión de tokens JWT")
public class AuthController {

    private final UserRepository   userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder  passwordEncoder;  // ← real ahora

    @Operation(
            summary     = "Login — obtener token JWT",
            description = "Verifica email + contraseña y devuelve un JWT. " +
                    "Incluirlo en: Authorization: Bearer <token>"
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                // Mensaje genérico — no revelamos si el email existe
                .orElseThrow(InvalidCredentialsException::new);

        // Verificación BCrypt real
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername());

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build());
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "El email es obligatorio.")
        @Email(message = "Formato de email inválido.")
        private String email;

        @NotBlank(message = "La contraseña es obligatoria.")
        private String password;
    }

    @Getter
    @Builder
    public static class LoginResponse {
        private final String token;
        private final Long   userId;
        private final String username;
    }
}