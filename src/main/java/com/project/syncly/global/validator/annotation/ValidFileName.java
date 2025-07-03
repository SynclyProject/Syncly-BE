package com.project.syncly.global.validator.annotation;

import com.project.syncly.global.validator.FileNameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = FileNameValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidFileName {
    String message() default "파일 이름 형식이 올바르지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}