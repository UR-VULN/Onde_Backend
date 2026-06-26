package com.onde.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordPolicyValidator implements ConstraintValidator<ValidPassword, String> {

    private PasswordPolicyLevel level;
    private boolean allowBlank;

    @Override
    public void initialize(ValidPassword annotation) {
        this.level = annotation.level();
        this.allowBlank = annotation.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return allowBlank;
        }
        return PasswordPolicy.validate(value, level)
                .map(message -> {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
                    return false;
                })
                .orElse(true);
    }
}
