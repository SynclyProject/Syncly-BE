package com.project.syncly.domain.member.entity;

import com.project.syncly.global.enums.BaseEnum;
import lombok.Getter;

@Getter
public enum LeaveReasonType implements BaseEnum {

    INCONVENIENT_SERVICE("INCONVENIENT_SERVICE", "서비스 이용이 불편해요"),
    MISSING_FEATURE("MISSING_FEATURE", "원하는 기능이 없어요"),
    LOW_USAGE("LOW_USAGE", "사용 빈도가 줄었어요"),
    UNSATISFACTORY_SUPPORT("UNSATISFACTORY_SUPPORT", "고객 지원이 만족스럽지 않았어요"),
    ETC("ETC", "기타");

    private final String key;
    private final String description;

    LeaveReasonType(String key, String description) {
        this.key = key;
        this.description = description;
    }

}

