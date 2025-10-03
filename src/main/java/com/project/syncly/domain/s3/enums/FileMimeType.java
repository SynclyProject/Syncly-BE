package com.project.syncly.domain.s3.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.project.syncly.domain.s3.exception.S3Exception;
import com.project.syncly.domain.s3.exception.S3ErrorCode;
import com.project.syncly.global.enums.BaseEnum;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileMimeType implements BaseEnum {
    // 이미지 파일
    JPG("image/jpeg", "jpg"),
    JPEG("image/jpeg", "jpeg"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    BMP("image/bmp", "bmp"),
    SVG("image/svg+xml", "svg"),
    WEBP("image/webp", "webp"),

    // 동영상 파일
    MP4("video/mp4", "mp4"),
    AVI("video/x-msvideo", "avi"),
    MKV("video/x-matroska", "mkv"),
    MOV("video/quicktime", "mov"),
    WMV("video/x-ms-wmv", "wmv"),
    FLV("video/x-flv", "flv"),
    WEBM("video/webm", "webm"),

    // 문서 파일
    PDF("application/pdf", "pdf"),
    DOC("application/msword", "doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    XLS("application/vnd.ms-excel", "xls"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PPT("application/vnd.ms-powerpoint", "ppt"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
    TXT("text/plain", "txt"),
    ;

    private final String key;
    private final String extension;

    FileMimeType(String key, String extension) {
        this.key = key;
        this.extension = extension;
    }

    public static FileMimeType extractMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        FileMimeType expected = fromExtension(ext);
        return expected;
    }

    // 확장자 -> mime (개발 단계: 모든 확장자 허용, 매칭되지 않으면 octet-stream으로 처리)
    public static FileMimeType fromExtension(String ext) {
        return switch (ext.toLowerCase()) {
            // 이미지 파일
            case "jpg" -> JPG;
            case "jpeg" -> JPEG;
            case "png" -> PNG;
            case "gif" -> GIF;
            case "bmp" -> BMP;
            case "svg" -> SVG;
            case "webp" -> WEBP;

            // 동영상 파일
            case "mp4" -> MP4;
            case "avi" -> AVI;
            case "mkv" -> MKV;
            case "mov" -> MOV;
            case "wmv" -> WMV;
            case "flv" -> FLV;
            case "webm" -> WEBM;

            // 문서 파일
            case "pdf" -> PDF;
            case "doc" -> DOC;
            case "docx" -> DOCX;
            case "xls" -> XLS;
            case "xlsx" -> XLSX;
            case "ppt" -> PPT;
            case "pptx" -> PPTX;
            case "txt" -> TXT;

            default -> TXT; // 알 수 없는 확장자는 text/plain으로 처리 (개발 단계)
        };
    }


    // JSON -> enum 역직렬화 시 사용
    @JsonCreator
    public static FileMimeType fromKey(String mimeType) {
        return Arrays.stream(values())
                .filter(e -> e.key.equalsIgnoreCase(mimeType))
                .findFirst()
                .orElseThrow(() -> new S3Exception(S3ErrorCode.UNSUPPORTED_MIME_TYPE));
    }

    // enum -> JSON 직렬화 시 사용
    @JsonValue
    public String getKey() {
        return key;
    }
}

