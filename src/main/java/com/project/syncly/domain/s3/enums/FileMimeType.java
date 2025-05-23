package com.project.syncly.domain.s3.enums;

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
}

