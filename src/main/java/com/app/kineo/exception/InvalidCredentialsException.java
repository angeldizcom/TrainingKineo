package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando la contraseña actual proporcionada no coincide.
 * Devuelve 401 con mensaje genérico para no revelar información
 * sobre el estado de la cuenta.
 */
public class InvalidCredentialsException extends KineoException {
    public InvalidCredentialsException() {
        super("Credenciales incorrectas.", HttpStatus.UNAUTHORIZED);
    }
}
