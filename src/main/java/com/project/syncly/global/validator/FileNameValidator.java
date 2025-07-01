package com.project.syncly.global.validator;

import com.project.syncly.global.validator.annotation.ValidFileName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileNameValidator implements ConstraintValidator<ValidFileName, String> {

    //한글/영문/기호/확장자 포함
    private static final String FILE_NAME_REGEX =
            "^[\\p{L}0-9 _\\-().\\[\\]]{1,50}\\.(jpg|jpeg|png)$";// image.jpg, 문서_파일(1).png 등 허용

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches(FILE_NAME_REGEX);
    }
}