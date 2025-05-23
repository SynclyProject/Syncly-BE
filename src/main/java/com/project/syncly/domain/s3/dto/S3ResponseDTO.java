package com.project.syncly.domain.s3.dto;

import lombok.Builder;

public class S3ResponseDTO {
    @Builder
    public record PreSignedUrl(
            String fileName,
            String uploadUrl,
            String objectKey
    ) {}
}
