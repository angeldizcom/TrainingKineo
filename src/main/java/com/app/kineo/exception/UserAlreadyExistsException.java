package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends KineoException {
    public UserAlreadyExistsException(String field, String value) {
        super("Ya existe un usuario con " + field + " = '" + value + "'.",
                HttpStatus.CONFLICT);
    }
}
