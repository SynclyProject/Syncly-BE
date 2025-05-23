package com.project.syncly.global.validator;

import com.project.syncly.domain.s3.enums.FileMimeType;
import com.project.syncly.global.validator.annotation.ValidMimeType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MimeTypeValidator implements ConstraintValidator<ValidMimeType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return FileMimeType.isValid(value);
    }
}