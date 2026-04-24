package com.app.kineo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración condicional de los modelos de IA.
 *
 * Controla qué modelos se activan según las properties:
 *
 *   kineo.ai.gemini.enabled=false   → Opción A: solo Claude
 *   kineo.ai.gemini.enabled=true    → Opción B: Claude + Gemini
 *
 * Cuando Gemini está desactivado:
 * · Sus dependencias pueden seguir en el pom.xml (comentadas)
 * · El autoconfigurador de Spring AI de Vertex NO se ejecuta
 * · TrainingManagerService recibe un Optional.empty() y usa Claude como fallback
 *
 * Para activar Gemini en el futuro solo hay que:
 * 1. Descomentar las dependencias en pom.xml
 * 2. Cambiar kineo.ai.gemini.enabled=true en application.properties
 * 3. Configurar GOOGLE_APPLICATION_CREDENTIALS en el entorno
 */
@Slf4j
@Configuration
public class AiProviderConfig {

    /**
     * Log de arranque para saber qué providers están activos.
     * Se ejecuta siempre, independientemente de la configuración.
     */
    @Bean
    public AiProviderStatus aiProviderStatus(
            @org.springframework.beans.factory.annotation.Value(
                    "${kineo.ai.gemini.enabled:false}") boolean geminiEnabled) {

        log.info("╔═══════════════════════════════════════╗");
        log.info("║       KINEO AI PROVIDERS STATUS       ║");
        log.info("╠═══════════════════════════════════════╣");
        log.info("║  Claude (Anthropic)  →  ✅ ACTIVO     ║");
        log.info("║  Gemini (Google)     →  {}  ║",
                geminiEnabled ? "✅ ACTIVO    " : "⏸ INACTIVO  ");
        log.info("╚═══════════════════════════════════════╝");

        return new AiProviderStatus(geminiEnabled);
    }

    public record AiProviderStatus(boolean geminiEnabled) {}
}
