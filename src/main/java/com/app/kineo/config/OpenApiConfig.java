package com.app.kineo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Añade el esquema bearerAuth a Swagger UI para que puedas probar
 * los endpoints protegidos directamente desde el navegador.
 *
 * Flujo en Swagger:
 * 1. POST /api/users/register  → crear usuario
 * 2. POST /api/auth/login      → obtener token
 * 3. Botón "Authorize" → pegar token → todos los endpoints protegidos funcionan
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title   = "Kineo Training Service API",
                version = "1.0",
                description = "Microservicio de entrenamiento inteligente con IA dual (Claude + Gemini).",
                contact = @Contact(name = "Kineo Team", email = "support@kineo.app")
        ),
        servers = @Server(description = "Local", url = "http://localhost:8080")
)
@SecurityScheme(
        name   = "bearerAuth",
        type   = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description  = "Introduce el token obtenido en POST /api/auth/login"
)
public class OpenApiConfig {}