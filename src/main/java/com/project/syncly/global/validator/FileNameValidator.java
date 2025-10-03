package com.project.syncly.global.validator;

import com.project.syncly.global.validator.annotation.ValidFileName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileNameValidator implements ConstraintValidator<ValidFileName, String> {

    //한글/영문/기호/확장자 포함 (개발 단계: 모든 확장자 허용)
    private static final String FILE_NAME_REGEX =
            "^[\\p{L}\\p{N} _\\-().\\[\\]]{1,100}\\.[a-zA-Z0-9]{1,10}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches(FILE_NAME_REGEX);
    }
}
