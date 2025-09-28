package com.project.syncly.global.validator;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.global.validator.annotation.PasswordMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, MemberRequestDTO.PasswordPair> {

    @Override
    public boolean isValid(MemberRequestDTO.PasswordPair dto, ConstraintValidatorContext context) {
        if (dto.newPassword() == null || dto.confirmPassword() == null) {
            return false;
        }
        return dto.newPassword().equals(dto.confirmPassword());
    }
}
