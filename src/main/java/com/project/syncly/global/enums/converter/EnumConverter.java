package com.project.syncly.global.enums.converter;

import com.project.syncly.global.enums.BaseEnum;
import com.project.syncly.global.enums.error.EnumErrorCode;
import com.project.syncly.global.enums.error.EnumException;

import java.util.Arrays;

//key 값으로 enum을 찾아줌
public class EnumConverter {
    public static <T extends Enum<T> & BaseEnum> T fromKey(Class<T> enumClass, String key) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new EnumException(EnumErrorCode.INVALID_ENUM_KEY));
    }
}