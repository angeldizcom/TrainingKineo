package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class NoAssessmentException extends KineoException {
    public NoAssessmentException(Long userId) {
        super("El usuario " + userId + " no tiene assessment. Completa el perfil antes de generar un plan.",
                HttpStatus.CONFLICT);
    }
}
