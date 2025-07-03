package com.project.syncly.global.enums.converter;

import com.project.syncly.global.enums.BaseEnum;
import com.project.syncly.global.enums.error.EnumErrorCode;
import com.project.syncly.global.enums.error.EnumException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

@Component
public class BaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        // 타입 캐스팅으로 컴파일러를 만족시키되, 안전하게 enum 타입 확인
        if (!Enum.class.isAssignableFrom(targetType)) {
            throw new EnumException(EnumErrorCode.ENUM_NOT_FOUND);
        }

        // 강제 캐스팅 후 전달
        @SuppressWarnings("unchecked")// 컴파일러의 경고를 억제, 제네릭 타입 강제 캐스팅의 안전을 개발자가 보장
        Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) targetType;

        return (Converter<String, T>) new StringToBaseEnumConverter(enumType);
    }

    private static class StringToBaseEnumConverter<T extends Enum<T> & BaseEnum> implements Converter<String, T> {

        private final Class<T> enumType;

        public StringToBaseEnumConverter(Class<T> enumType) {
            this.enumType = enumType;
        }

        @Override
        public T convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            return EnumConverter.fromKey(enumType, source);
        }
    }
}
