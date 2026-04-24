package com.app.kineo.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Principal custom que Spring Security guarda en el SecurityContext
 * tras validar el token JWT.
 *
 * Los controllers lo reciben con:
 *   @AuthenticationPrincipal KineoPrincipal principal
 *
 * Esto elimina userId como @RequestParam en todos los endpoints
 * que requieren autenticación — el userId viene del token, no del cliente.
 */
@Getter
@RequiredArgsConstructor
public class KineoPrincipal {
    private final Long   userId;
    private final String username;
}
