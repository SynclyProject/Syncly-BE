package com.project.syncly.global.validator.annotation;

import com.project.syncly.global.validator.MimeTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = MimeTypeValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidMimeType {
    String message() default "지원하지 않는 MIME 타입입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
