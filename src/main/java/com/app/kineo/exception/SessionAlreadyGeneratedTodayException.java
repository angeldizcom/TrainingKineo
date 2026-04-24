package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class SessionAlreadyGeneratedTodayException extends KineoException {
    public SessionAlreadyGeneratedTodayException(Long planId) {
        super("Ya se generó una sesión hoy para el plan " + planId + ". " +
                        "Completa la sesión actual antes de generar la siguiente.",
                HttpStatus.TOO_MANY_REQUESTS);
    }
}
