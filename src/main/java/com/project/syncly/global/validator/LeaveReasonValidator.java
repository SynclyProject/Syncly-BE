package com.project.syncly.global.validator;

import com.project.syncly.domain.member.dto.request.MemberRequestDTO;
import com.project.syncly.domain.member.entity.LeaveReasonType;
import com.project.syncly.global.validator.annotation.ValidLeaveReason;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LeaveReasonValidator implements ConstraintValidator<ValidLeaveReason, MemberRequestDTO.DeleteMember> {

    @Override
    public boolean isValid(MemberRequestDTO.DeleteMember dto, ConstraintValidatorContext context) {
        LeaveReasonType type = dto.leaveReasonType();
        // Null이면 커스텀 메시지 설정하고 false 반환
        if (type == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("탈퇴 사유를 선택해주세요.")
                    .addPropertyNode("leaveReasonType")
                    .addConstraintViolation();
            return false;
        }

        // ETC일 경우에만 leaveReason 필수
        if (type == LeaveReasonType.ETC) {
            if (dto.leaveReason() == null || dto.leaveReason().trim().isEmpty()) {//null 이거나 공백문자열이라면
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("기타 사유를 입력해주세요.")
                        .addPropertyNode("leaveReason")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}

