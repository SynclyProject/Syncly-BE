package com.project.syncly.domain.file.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.project.syncly.global.enums.BaseEnum;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileType implements BaseEnum {
    FOLDER("FOLDER"),
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    FILE("FILE");

    private final String key;

    FileType(String key) {
        this.key = key;
    }

    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null) return FILE;

        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) return IMAGE;
        if (type.startsWith("video/")) return VIDEO;

        return FILE;
    }

    public static FileType fromExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return FILE;

        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> IMAGE;
            case "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> VIDEO;
            default -> FILE;
        };
    }

    @JsonCreator
    public static FileType fromKey(String key) {
        return Arrays.stream(values())
                .filter(e -> e.key.equalsIgnoreCase(key))
                .findFirst()
                .orElse(FILE);
    }

    @JsonValue
    public String getKey() {
        return key;
    }
}