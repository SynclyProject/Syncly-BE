package com.project.syncly.domain.member.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {
    // 사용자 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404_01", "해당 회원이 존재하지 않습니다."),
    MEMBER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "MEMBER404_02", "해당 이메일로 가입된 회원이 존재하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER409_01", "이미 가입된 이메일입니다."),
    INVALID_AUTH_CODE(HttpStatus.BAD_REQUEST, "MEMBER400_01", "인증 코드가 올바르지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "MEMBER400_02", "이메일 인증이 완료되지 않았습니다."),
    PASSWORD_NOT_MATCHED(HttpStatus.BAD_REQUEST, "MEMBER400_03", "이메일 혹은 비밀번호가 잘못되었습니다."),
    NO_LEAVE_REASON_TYPE(HttpStatus.BAD_REQUEST, "MEMBER400_04", "잘못된 탈퇴사유 타입입니다."),
    SOCIAL_MEMBER_CANNOT_USE_THIS_FEATURE(HttpStatus.BAD_REQUEST,"MEMBER400_05", "소셜 로그인 사용자는 이 기능을 이용할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}