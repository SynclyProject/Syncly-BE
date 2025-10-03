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

    // 오디오 파일
    MP3("audio/mpeg", "mp3"),
    WAV("audio/wav", "wav"),
    OGG("audio/ogg", "ogg"),
    M4A("audio/mp4", "m4a"),
    FLAC("audio/flac", "flac"),
    AAC("audio/aac", "aac"),

    // 문서 파일
    PDF("application/pdf", "pdf"),
    DOC("application/msword", "doc"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
    XLS("application/vnd.ms-excel", "xls"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PPT("application/vnd.ms-powerpoint", "ppt"),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
    TXT("text/plain", "txt"),
    RTF("application/rtf", "rtf"),
    CSV("text/csv", "csv"),
    HWP("application/x-hwp", "hwp"),
    HWPX("application/hwp+zip", "hwpx"),

    // 압축 파일
    ZIP("application/zip", "zip"),
    RAR("application/vnd.rar", "rar"),
    TAR("application/x-tar", "tar"),
    GZ("application/gzip", "gz"),
    SEVENZ("application/x-7z-compressed", "7z"),

    // 코드/개발 파일
    JSON("application/json", "json"),
    XML("application/xml", "xml"),
    HTML("text/html", "html"),
    CSS("text/css", "css"),
    JS("text/javascript", "js"),
    TS("text/typescript", "ts"),
    JSX("text/jsx", "jsx"),
    TSX("text/tsx", "tsx"),
    JAVA("text/x-java-source", "java"),
    PY("text/x-python", "py"),
    CPP("text/x-c++src", "cpp"),
    C("text/x-csrc", "c"),
    SH("application/x-sh", "sh"),
    MD("text/markdown", "md"),
    YML("application/x-yaml", "yml"),
    YAML("application/x-yaml", "yaml"),

    // 기타
    SQL("application/sql", "sql"),
    EXE("application/vnd.microsoft.portable-executable", "exe"),
    APK("application/vnd.android.package-archive", "apk"),
    IPA("application/octet-stream", "ipa"),
    DMG("application/x-apple-diskimage", "dmg"),
    ISO("application/x-iso9660-image", "iso"),
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

            // 오디오 파일
            case "mp3" -> MP3;
            case "wav" -> WAV;
            case "ogg" -> OGG;
            case "m4a" -> M4A;
            case "flac" -> FLAC;
            case "aac" -> AAC;

            // 문서 파일
            case "pdf" -> PDF;
            case "doc" -> DOC;
            case "docx" -> DOCX;
            case "xls" -> XLS;
            case "xlsx" -> XLSX;
            case "ppt" -> PPT;
            case "pptx" -> PPTX;
            case "txt" -> TXT;
            case "rtf" -> RTF;
            case "csv" -> CSV;
            case "hwp" -> HWP;
            case "hwpx" -> HWPX;

            // 압축 파일
            case "zip" -> ZIP;
            case "rar" -> RAR;
            case "tar" -> TAR;
            case "gz" -> GZ;
            case "7z" -> SEVENZ;

            // 코드/개발 파일
            case "json" -> JSON;
            case "xml" -> XML;
            case "html" -> HTML;
            case "css" -> CSS;
            case "js" -> JS;
            case "ts" -> TS;
            case "jsx" -> JSX;
            case "tsx" -> TSX;
            case "java" -> JAVA;
            case "py" -> PY;
            case "cpp" -> CPP;
            case "c" -> C;
            case "sh" -> SH;
            case "md" -> MD;
            case "yml" -> YML;
            case "yaml" -> YAML;

            // 기타
            case "sql" -> SQL;
            case "exe" -> EXE;
            case "apk" -> APK;
            case "ipa" -> IPA;
            case "dmg" -> DMG;
            case "iso" -> ISO;

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

