package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class ExerciseNotFoundException extends KineoException {
    public ExerciseNotFoundException(Long exerciseId) {
        super("Ejercicio " + exerciseId + " no encontrado.", HttpStatus.NOT_FOUND);
    }
}
