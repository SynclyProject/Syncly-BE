package com.project.syncly.domain.s3.dto;

import com.project.syncly.domain.s3.enums.FileMimeType;
import com.project.syncly.global.validator.annotation.ValidFileName;
import com.project.syncly.global.validator.annotation.ValidMimeMatch;
import jakarta.validation.constraints.NotBlank;

public class S3RequestDTO {
    public interface UploadPreSignedUrl {
        String fileName();
        FileMimeType mimeType();
    }
    @ValidMimeMatch
    public record ProfileImageUploadPreSignedUrl(
            @ValidFileName String fileName,
            FileMimeType mimeType
    ) implements UploadPreSignedUrl {}

    @ValidMimeMatch
    public record DriveFileUploadPreSignedUrl (
            Long folderId,
            Long memberId,
            @ValidFileName String fileName,
            FileMimeType mimeType
    ) implements UploadPreSignedUrl {}


    public record UpdateFile(
            @NotBlank String fileName,
            @NotBlank String objectKey
    ) {}

    public record GetViewUrl(
            @NotBlank String objectKey
    ) {}

    public record GetDownloadUrl(
            //여기 그냥 파일아이디만 받고 파일에 저 내용들 넣어두는 방식이 좋을 듯
            @NotBlank String objectKey,
            @ValidFileName String fileName,
            @NotBlank Long fileId
    ) {}
}
