package com.app.kineo.exception;

import org.springframework.http.HttpStatus;

public class PlanNotFoundException extends KineoException {
    public PlanNotFoundException(Long planId) {
        super("Plan " + planId + " no encontrado.", HttpStatus.NOT_FOUND);
    }
}
