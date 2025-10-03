package com.project.syncly.global.validator;

import com.project.syncly.global.validator.annotation.ValidFileName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FileNameValidator implements ConstraintValidator<ValidFileName, String> {

    // 개발 단계: 파일명 제한 완화 (확장자만 필수, 최대 255자)
    private static final String FILE_NAME_REGEX =
            "^.{1,255}\\..+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        // 확장자가 있는지만 확인 (개발 단계)
        return value.contains(".") && value.lastIndexOf('.') > 0 && value.lastIndexOf('.') < value.length() - 1;
    }
}
