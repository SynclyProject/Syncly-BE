package com.project.syncly.domain.s3.dto;

import com.project.syncly.domain.s3.enums.FileMimeType;
import com.project.syncly.domain.s3.exception.S3ErrorCode;
import com.project.syncly.domain.s3.exception.S3Exception;
import com.project.syncly.global.validator.annotation.ValidFileName;
import com.project.syncly.global.validator.annotation.ValidMimeType;
import jakarta.validation.constraints.NotBlank;

public class S3RequestDTO {
    public record PreSignedUrl(
            @ValidFileName String fileName,
            @ValidMimeType String mimeType
    ) {
        public void isFileNameAndMimeTypeMatch() {
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
            FileMimeType expected = FileMimeType.fromExtension(ext);
            FileMimeType actual = FileMimeType.fromMimeType(mimeType);

            if (expected != actual) {
                throw new S3Exception(S3ErrorCode.MIME_TYPE_MISMATCH);
            }
        }
    }

    public record UpdateFile(
            @NotBlank String fileName,
            @NotBlank String objectKey
    ) {}
}
