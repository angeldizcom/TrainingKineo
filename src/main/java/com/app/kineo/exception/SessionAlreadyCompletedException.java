package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class SessionAlreadyCompletedException extends KineoException {
    public SessionAlreadyCompletedException(String sessionName) {
        super("La sesión '" + sessionName + "' ya está completada y no admite cambios.",
                HttpStatus.CONFLICT);
    }
}
