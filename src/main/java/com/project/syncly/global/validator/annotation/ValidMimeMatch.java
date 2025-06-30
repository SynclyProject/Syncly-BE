package com.project.syncly.global.validator.annotation;

import com.project.syncly.global.validator.MimeMatchValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MimeMatchValidator.class)
@Target({ ElementType.TYPE }) // DTO 전체를 검증
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMimeMatch {
    String message() default "파일 확장자와 MIME 타입이 일치하지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}