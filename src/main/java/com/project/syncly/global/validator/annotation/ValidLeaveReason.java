package com.project.syncly.global.validator.annotation;

import com.project.syncly.global.validator.LeaveReasonValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LeaveReasonValidator.class)
public @interface ValidLeaveReason {
    String message() default "기타 사유 선택 시 내용을 입력해주세요.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}