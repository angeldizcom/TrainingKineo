package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends KineoException {
    public UserNotFoundException(Long userId) {
        super("Usuario " + userId + " no encontrado.", HttpStatus.NOT_FOUND);
    }
}
