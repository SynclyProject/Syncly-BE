package com.project.syncly.domain.file.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.project.syncly.global.enums.BaseEnum;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileType implements BaseEnum {
    IMAGE("IMAGE"),
    DOCUMENT("DOCUMENT"),
    VIDEO("VIDEO"),
    AUDIO("AUDIO"),
    ARCHIVE("ARCHIVE"),
    OTHER("OTHER");

    private final String key;

    FileType(String key) {
        this.key = key;
    }

    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null) return OTHER;
        
        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) return IMAGE;
        if (type.startsWith("video/")) return VIDEO;
        if (type.startsWith("audio/")) return AUDIO;
        if (type.contains("pdf") || type.contains("document") || type.contains("text") || 
            type.contains("spreadsheet") || type.contains("presentation")) return DOCUMENT;
        if (type.contains("zip") || type.contains("rar") || type.contains("tar") || 
            type.contains("gzip")) return ARCHIVE;
        
        return OTHER;
    }

    public static FileType fromExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return OTHER;
        
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> IMAGE;
            case "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> VIDEO;
            case "mp3", "wav", "flac", "aac", "ogg", "wma" -> AUDIO;
            case "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf" -> DOCUMENT;
            case "zip", "rar", "7z", "tar", "gz", "bz2" -> ARCHIVE;
            default -> OTHER;
        };
    }

    @JsonCreator
    public static FileType fromKey(String key) {
        return Arrays.stream(values())
                .filter(e -> e.key.equalsIgnoreCase(key))
                .findFirst()
                .orElse(OTHER);
    }

    @JsonValue
    public String getKey() {
        return key;
    }
}