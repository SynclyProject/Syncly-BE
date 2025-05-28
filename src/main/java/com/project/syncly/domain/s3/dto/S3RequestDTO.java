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
            FileMimeType mimeType)
            implements MimeMatchValidatable {

            if (expected != actual) {
                throw new S3Exception(S3ErrorCode.MIME_TYPE_MISMATCH);
            }
        }
    }

    public record UpdateFile(
            @NotBlank String fileName,
            @NotBlank String objectKey
    ) {}

    public record GetViewUrl(
            @NotBlank String objectKey,
            FileMimeType mimeType
    ) {}

    public record GetDownloadUrl(
            //여기 그냥 파일아이디만 받고 파일에 저 내용들 넣어두는 방식이 좋을 듯
            @NotBlank String objectKey,
            FileMimeType mimeType,
            @ValidFileName String fileName,
            @NotBlank Long fileId
    ) {}
}
