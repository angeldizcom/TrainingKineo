package com.app.kineo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Genera, firma y valida tokens JWT para la API de Kineo.
 *
 * El token contiene:
 * · sub  — userId (Long como String)
 * · usr  — username
 * · iat  — issued at
 * · exp  — expiration
 *
 * Configuración en application.properties:
 *   kineo.jwt.secret=<clave de mínimo 64 caracteres>
 *   kineo.jwt.expiration-ms=86400000   # 24h
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long      expirationMs;

    public JwtTokenProvider(
            @Value("${kineo.jwt.secret}") String secret,
            @Value("${kineo.jwt.expiration-ms:86400000}") long expirationMs) {

        // HMAC-SHA512 requiere clave >= 64 bytes
        if (secret.length() < 64) {
            throw new IllegalArgumentException(
                    "kineo.jwt.secret debe tener al menos 64 caracteres.");
        }
        this.key          = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ─────────────────────────────────────────────────────────
    // Generación
    // ─────────────────────────────────────────────────────────

    /**
     * Genera un token JWT firmado con HS512.
     *
     * @param userId   ID del usuario autenticado.
     * @param username Nombre de usuario (claim extra para logging).
     */
    public String generateToken(Long userId, String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("usr", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    // ─────────────────────────────────────────────────────────
    // Extracción
    // ─────────────────────────────────────────────────────────

    /** Extrae el userId del subject del token. */
    public Long extractUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** Extrae el username del claim custom. */
    public String extractUsername(String token) {
        return parseClaims(token).get("usr", String.class);
    }

    // ─────────────────────────────────────────────────────────
    // Validación
    // ─────────────────────────────────────────────────────────

    /**
     * Valida el token y devuelve true si es correcto.
     * Los errores se loguean como WARN — nunca se propagan como excepción
     * desde aquí para que el filtro pueda devolver 401 limpio.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] Token expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] Token no soportado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Token malformado: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("[JWT] Firma JWT inválida: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] Token vacío o nulo: {}", e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
