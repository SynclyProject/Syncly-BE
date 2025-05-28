package com.project.syncly.domain.s3.enums;

import com.project.syncly.domain.s3.exception.S3Exception;
import com.project.syncly.domain.s3.exception.S3ErrorCode;
import com.project.syncly.global.enums.BaseEnum;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileMimeType implements BaseEnum {
    JPG("image/jpg"),
    JPEG("image/jpeg"),
    PNG("image/png"),
    ;

    private final String key;

    FileMimeType(String key) {
        this.key = key;
    }

    public static FileMimeType extractMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        FileMimeType expected = fromExtension(ext);
        return expected;
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
                .filter(e -> e.key.equalsIgnoreCase(mimeType))
                .findFirst()
                .orElseThrow(() -> new S3Exception(S3ErrorCode.UNSUPPORTED_MIME_TYPE));
    }
}

