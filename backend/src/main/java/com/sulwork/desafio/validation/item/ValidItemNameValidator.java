package com.sulwork.desafio.validation.item;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidItemNameValidator implements ConstraintValidator<ValidItemName, String> {

    private static final int MAXIMUM_LENGTH = 120;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && !value.isBlank() && value.length() <= MAXIMUM_LENGTH;
    }
}
