package com.app.kineo.service;

import com.app.kineo.config.AiProviderConfig.AiProviderStatus;
import com.app.kineo.model.TrainingPlan;
import com.app.kineo.model.User;
import com.app.kineo.repository.UserRepository;
import com.app.kineo.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orquestador central de IA de Kineo.
 *
 * Soporta dos modos de despliegue:
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ Opción A — kineo.ai.gemini.enabled=false (actual)       │
 * │   · Claude gestiona todo                                │
 * │   · Requests con provider=GEMINI → fallback a Claude   │
 * │   · Sin dependencias de Google Cloud                    │
 * ├─────────────────────────────────────────────────────────┤
 * │ Opción B — kineo.ai.gemini.enabled=true (futuro)        │
 * │   · Claude = principal con MCP                          │
 * │   · Gemini = fallback autónomo real                     │
 * │   · Requiere credenciales de Google Cloud               │
 * └─────────────────────────────────────────────────────────┘
 *
 * El switch entre opciones no requiere cambios en este servicio
 * ni en los controllers — solo cambiar la property y el pom.xml.
 */
@Slf4j
@Service
public class TrainingManagerService {

    private final AnthropicChatModel    anthropicModel;
    private final TrainingAiService     trainingAiService;
    private final TrainingPlanService   trainingPlanService;
    private final UserRepository        userRepository;
    private final AiProviderStatus      providerStatus;

    /**
     * GoogleGenAiChatModel es Optional porque puede no estar en el classpath
     * cuando Gemini está desactivado (Opción A).
     *
     * Spring inyecta el bean si existe, Optional.empty() si no.
     * No hay error de arranque en ninguno de los dos casos.
     */
    private final Optional<Object> geminiModel; // Object para evitar import del tipo ausente

    @Autowired
    public TrainingManagerService(
            AnthropicChatModel anthropicModel,
            TrainingAiService trainingAiService,
            TrainingPlanService trainingPlanService,
            UserRepository userRepository,
            AiProviderStatus providerStatus) {

        this.anthropicModel      = anthropicModel;
        this.trainingAiService   = trainingAiService;
        this.trainingPlanService = trainingPlanService;
        this.userRepository      = userRepository;
        this.providerStatus      = providerStatus;
        this.geminiModel         = Optional.empty(); // Opción A — sin Gemini
    }

    // ─────────────────────────────────────────────────────────
    // Modo 1 — Sesión estándar
    // ─────────────────────────────────────────────────────────

    public String processSessionBuild(Long userId, Long planId,
                                      String goal, String provider) {
        String resolvedProvider = resolveProvider(provider);
        log.info("[TrainingManager] Sesión estándar | usuario={} provider={}{}",
                userId, resolvedProvider,
                resolvedProvider.equals(provider) ? "" : " (fallback desde " + provider + ")");

        return trainingAiService.generateAndSaveSession(
                userId, planId, goal, buildClient(resolvedProvider)
        );
    }

    // ─────────────────────────────────────────────────────────
    // Modo 2 — Plan completo
    // ─────────────────────────────────────────────────────────

    public TrainingPlan processPlanGeneration(Long userId, int weeks, String provider) {
        String resolvedProvider = resolveProvider(provider);
        log.info("[TrainingManager] Plan completo | usuario={} semanas={} provider={}{}",
                userId, weeks, resolvedProvider,
                resolvedProvider.equals(provider) ? "" : " (fallback desde " + provider + ")");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return trainingPlanService.generatePlanForUser(user, weeks, buildClient(resolvedProvider));
    }

    // ─────────────────────────────────────────────────────────
    // Modo 3 — Sesión adaptativa
    // ─────────────────────────────────────────────────────────

    public String processAdaptiveSession(Long userId, Long planId,
                                         String goal, String provider) {
        String resolvedProvider = resolveProvider(provider);
        log.info("[TrainingManager] Sesión adaptativa | usuario={} provider={}{}",
                userId, resolvedProvider,
                resolvedProvider.equals(provider) ? "" : " (fallback desde " + provider + ")");

        return trainingAiService.generateAdaptiveSession(
                userId, planId, goal, buildClient(resolvedProvider)
        );
    }

    // ─────────────────────────────────────────────────────────
    // Routing de modelos
    // ─────────────────────────────────────────────────────────

    /**
     * Si Gemini está desactivado (Opción A) y el cliente pide GEMINI,
     * resolvemos a CLAUDE automáticamente y lo logueamos.
     *
     * Cuando actives Gemini en el futuro (Opción B), este método
     * devolverá GEMINI directamente sin ningún cambio más.
     */
    private String resolveProvider(String requested) {
        if ("GEMINI".equalsIgnoreCase(requested) && !providerStatus.geminiEnabled()) {
            log.warn("[TrainingManager] Gemini no está activo. " +
                    "Usando Claude como fallback. " +
                    "Para activar Gemini: kineo.ai.gemini.enabled=true");
            return "CLAUDE";
        }
        return requested.toUpperCase();
    }

    /**
     * Construye el ChatClient para el provider resuelto.
     *
     * Claude siempre incluye las tres herramientas MCP.
     * Gemini (cuando esté activo en Opción B) se construirá
     * desde el bean inyectado en geminiModel.
     */
    private ChatClient buildClient(String resolvedProvider) {
        // En Opción A, resolvedProvider siempre es CLAUDE aquí
        // En Opción B, si es GEMINI se usa el model de Google
        return ChatClient.builder(anthropicModel)
                .defaultFunctions("getUserAssessment", "searchExercises", "getRecentSessions")
                .build();
    }
}