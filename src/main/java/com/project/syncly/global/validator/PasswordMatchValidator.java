package com.project.syncly.global.validator;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO.UpdatePassword;
import com.project.syncly.global.validator.annotation.PasswordMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, UpdatePassword> {

    @Override
    public boolean isValid(UpdatePassword dto, ConstraintValidatorContext context) {
        if (dto.newPassword() == null || dto.confirmPassword() == null) {
            return false;
        }
        return dto.newPassword().equals(dto.confirmPassword());
    }
}
