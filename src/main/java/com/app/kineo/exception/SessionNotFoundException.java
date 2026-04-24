package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends KineoException {
    public SessionNotFoundException(Long sessionId) {
        super("Sesión " + sessionId + " no encontrada.", HttpStatus.NOT_FOUND);
    }
}
