package com.app.kineo.service;

import com.app.kineo.exception.AiParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Parsea y valida respuestas JSON de la IA con un mecanismo de reintento.
 *
 * <p><b>Flujo:</b></p>
 * <pre>
 *   1ª llamada a la IA → rawResponse
 *        ↓
 *   cleanJson() + parse()
 *        ↓ falla
 *   Prompt de corrección a la IA con el JSON roto
 *        ↓
 *   2ª llamada → correctedResponse
 *        ↓ falla
 *   AiParseException (422) — se loguea el JSON original para debug
 * </pre>
 *
 * <p>El reintento no invoca al modelo de negocio de nuevo — solo pide a la IA
 * que corrija el formato del JSON que ya generó. Es barato en tokens y resuelve
 * el 90% de los fallos (markdown residual, comas finales, campos extra).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────

    /**
     * Intenta deserializar {@code rawResponse} al tipo indicado.
     * Si falla, pide a la IA que corrija el JSON y reintenta una vez.
     * Si el segundo intento también falla lanza {@link AiParseException}.
     *
     * @param rawResponse  Texto devuelto por la IA (puede tener markdown, etc.).
     * @param targetClass  Clase Java a la que deserializar.
     * @param chatClient   Cliente AI para el reintento de corrección.
     * @param <T>          Tipo de respuesta esperado.
     */
    public <T> T parseWithRetry(String rawResponse, Class<T> targetClass, ChatClient chatClient) {
        // Intento 1 — parseo directo tras limpiar markdown
        String cleaned = cleanJson(rawResponse);
        try {
            T result = objectMapper.readValue(cleaned, targetClass);
            log.debug("[AiResponseParser] Parse OK en primer intento para {}.",
                    targetClass.getSimpleName());
            return result;
        } catch (JsonProcessingException firstEx) {
            log.warn("[AiResponseParser] Primer intento fallido para {}. Iniciando reintento de corrección. Error: {}",
                    targetClass.getSimpleName(), firstEx.getMessage());
        }

        // Intento 2 — pedimos a la IA que corrija su propio JSON
        String correctedRaw = requestJsonCorrection(rawResponse, targetClass, chatClient);
        String correctedCleaned = cleanJson(correctedRaw);

        try {
            T result = objectMapper.readValue(correctedCleaned, targetClass);
            log.info("[AiResponseParser] Parse OK en segundo intento (corrección) para {}.",
                    targetClass.getSimpleName());
            return result;
        } catch (JsonProcessingException secondEx) {
            log.error("[AiResponseParser] Segundo intento fallido para {}.\n" +
                      "JSON original:   {}\n" +
                      "JSON corregido:  {}",
                    targetClass.getSimpleName(), rawResponse, correctedRaw);
            throw new AiParseException(rawResponse, secondEx);
        }
    }

    // ─────────────────────────────────────────────────────────
    // Corrección via IA
    // ─────────────────────────────────────────────────────────

    /**
     * Construye un prompt que le muestra a la IA su propio JSON roto
     * y le pide exclusivamente la versión corregida, sin explicaciones.
     *
     * El esquema JSON esperado se deriva de los campos de {@code targetClass}
     * para que la corrección sea informada, no a ciegas.
     */
    private <T> String requestJsonCorrection(String brokenJson,
                                              Class<T> targetClass,
                                              ChatClient chatClient) {
        String schema = buildSchemaHint(targetClass);

        String correctionPrompt = String.format("""
                El siguiente JSON tiene errores de formato y no se puede parsear.
                Corrígelo para que sea JSON válido que cumpla este esquema:

                %s

                JSON con errores:
                %s

                Devuelve ÚNICAMENTE el JSON corregido, sin explicaciones, sin markdown, sin texto extra.
                Si no es posible corregirlo, devuelve el JSON mínimo válido que cumpla el esquema.
                """, schema, brokenJson);

        return chatClient.prompt()
                .user(correctionPrompt)
                .call()
                .content();
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Elimina bloques de markdown que los modelos insertan a veces
     * a pesar de que el prompt los prohíbe explícitamente.
     * También hace trim de espacios y saltos de línea iniciales/finales.
     */
    public String cleanJson(String response) {
        if (response == null || response.isBlank()) return "{}";
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) return trimmed.substring(7, trimmed.lastIndexOf("```")).trim();
        if (trimmed.startsWith("```"))     return trimmed.substring(3, trimmed.lastIndexOf("```")).trim();
        return trimmed;
    }

    /**
     * Genera una descripción textual del esquema esperado a partir de
     * los campos de la clase Java. Es suficientemente informativa para
     * que la IA sepa qué estructura debe producir en el reintento.
     *
     * No usa reflection compleja — solo lista los campos declarados con su tipo.
     */
    private <T> String buildSchemaHint(Class<T> targetClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("Clase: ").append(targetClass.getSimpleName()).append("\n");
        sb.append("Campos esperados:\n");

        for (var field : targetClass.getDeclaredFields()) {
            sb.append("  - ")
              .append(field.getName())
              .append(" (")
              .append(field.getType().getSimpleName())
              .append(")\n");
        }

        return sb.toString();
    }
}
