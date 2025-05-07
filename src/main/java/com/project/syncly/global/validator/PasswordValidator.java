package com.project.syncly.global.validator;

import com.project.syncly.global.validator.annotation.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // 비밀번호는 8~20자, 영문자 + 숫자 + 특수문자 조합 필수 (공백 불가)
    private static final String PASSWORD_REGEX =
            "^" + // 문자열 시작
                    "(?=.*[A-Za-z])" + // 영문자 (대소문자 구분 없이) 1개 이상 포함
                    "(?=.*\\d)" + // 숫자 1개 이상 포함
                    "(?=.*[!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?])" + // 특수문자 1개 이상 포함
                    "[A-Za-z\\d!@#$%^&*()_+=\\-\\[\\]{};':\"\\\\|,.<>/?]{8,20}" + // 전체 길이 8~20자, 허용된 문자만 사용
                    "$"; // 문자열 끝


    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        return password.matches(PASSWORD_REGEX);
    }
}