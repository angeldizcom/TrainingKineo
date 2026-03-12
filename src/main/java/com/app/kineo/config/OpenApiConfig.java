package com.app.kineo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Kineo Training Service API",
                version = "1.0",
                description = "Microservicio para la gestión de entrenamientos y generación de planes mediante IA (Gemini).",
                contact = @Contact(
                        name = "Kineo Team",
                        email = "support@kineo.app"
                )
        ),
        servers = {
                @Server(
                        description = "Local Environment",
                        url = "http://localhost:8080"
                )
        }
)
public class OpenApiConfig {
}
