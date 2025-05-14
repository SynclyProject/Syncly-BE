package com.project.syncly.domain.member.entity;

import com.project.syncly.domain.auth.oauth.exception.AuthErrorCode;
import com.project.syncly.domain.auth.oauth.exception.AuthException;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;

public enum LeaveReasonType {

    INCONVENIENT_SERVICE("서비스 이용이 불편해요"),
    MISSING_FEATURE("원하는 기능이 없어요"),
    LOW_USAGE("사용 빈도가 줄었어요"),
    UNSATISFACTORY_SUPPORT("고객 지원이 만족스럽지 않았어요"),
    ETC("기타");

    private final String description;

    LeaveReasonType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static LeaveReasonType from(String description) {
        for (LeaveReasonType reason : LeaveReasonType.values()) {
            if (reason.description.equals(description)) {
                return reason;
            }
        }
        throw new MemberException(MemberErrorCode.NO_LEAVE_REASON_TYPE);
    }

}
