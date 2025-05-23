package com.project.syncly.domain.s3.dto;

import com.project.syncly.global.validator.annotation.ValidFileName;
import com.project.syncly.global.validator.annotation.ValidMimeType;
import jakarta.validation.constraints.NotBlank;

public class S3RequestDTO {
    public record PreSignedUrl(
            @ValidFileName String fileName,
            @ValidMimeType String mimeType
    ) {}

    public record UpdateFile(
            @NotBlank String fileName,
            @NotBlank String objectKey
    ) {}
}
