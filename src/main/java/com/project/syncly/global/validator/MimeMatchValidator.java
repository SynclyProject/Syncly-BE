package com.project.syncly.global.validator;

import com.project.syncly.domain.s3.dto.S3RequestDTO.PreSignedUrl;
import com.project.syncly.domain.s3.enums.FileMimeType;
import com.project.syncly.global.validator.annotation.ValidMimeMatch;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MimeMatchValidator implements ConstraintValidator<ValidMimeMatch, PreSignedUrl> {

    @Override
    public boolean isValid(PreSignedUrl dto, ConstraintValidatorContext context) {
        if (dto == null || dto.fileName() == null || dto.mimeType() == null) {
            return false;
        }
        try {
            FileMimeType expected = FileMimeType.extractMimeType(dto.fileName());
            FileMimeType requestMimeType = dto.mimeType();
            return expected == requestMimeType;
        } catch (Exception e) {
            return false;
        }
    }
}