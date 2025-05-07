package com.project.syncly.global.jwt.enums;

import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;

import java.util.Arrays;

public enum TokenType {
    ACCESS, REFRESH;

    public static TokenType from(String type) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new JwtException(JwtErrorCode.INVALID_TOKEN));
    }
}