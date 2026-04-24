package com.app.kineo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Set;

/**
 * Valida que el parámetro `provider` de los endpoints de generación
 * sea uno de los valores aceptados por el sistema (CLAUDE | GEMINI).
 *
 * Uso en controller:
 *   public ResponseEntity<?> generate(@RequestParam @ValidProvider String provider)
 *
 * Si en el futuro se añade un nuevo modelo (LLAMA, GPT...) solo hay que
 * añadirlo a ACCEPTED_PROVIDERS — los controllers no cambian.
 */
@Documented
@Constraint(validatedBy = ValidProvider.Validator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProvider {

    String message() default "Provider no válido. Valores aceptados: CLAUDE, GEMINI.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidProvider, String> {

        private static final Set<String> ACCEPTED_PROVIDERS = Set.of("CLAUDE", "GEMINI");

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) return true; // null se controla con @NotNull si hace falta
            return ACCEPTED_PROVIDERS.contains(value.toUpperCase());
        }
    }
}
