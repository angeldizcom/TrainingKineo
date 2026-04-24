package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class SetNotFoundException extends KineoException {
    public SetNotFoundException(Long setId) {
        super("Serie " + setId + " no encontrada.", HttpStatus.NOT_FOUND);
    }
}
