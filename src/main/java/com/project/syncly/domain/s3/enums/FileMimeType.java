package com.project.syncly.domain.s3.enums;

import com.project.syncly.domain.s3.exception.S3Exception;
import com.project.syncly.domain.s3.exception.S3ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileMimeType {
    JPG("image/jpg"),
    JPEG("image/jpeg"),
    PNG("image/png"),
    ;

    private final String mimeType;

    FileMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public static boolean isValid(String input) {
        return Arrays.stream(values())
                .anyMatch(e -> e.mimeType.equalsIgnoreCase(input));
    }

    // 확장자 -> mime
    public static FileMimeType fromExtension(String ext) {
        return switch (ext.toLowerCase()) {
            case "jpg" -> JPG;
            case "jpeg" -> JPEG;
            case "png" -> PNG;
            default -> throw new S3Exception(S3ErrorCode.UNSUPPORTED_FILE_EXTENSION);
        };
    }

    public static FileMimeType fromMimeType(String mimeType) {
        return Arrays.stream(values())
                .filter(e -> e.mimeType.equalsIgnoreCase(mimeType))
                .findFirst()
                .orElseThrow(() -> new S3Exception(S3ErrorCode.UNSUPPORTED_MIME_TYPE));
    }
}

