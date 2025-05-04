package com.project.syncly.domain.member.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {
    // 사용자 관련 에러
    PROFILE_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "MEMBER4001", "필수 정보를 입력해주세요."),
    INPUT_ENTER_REQURIED_INFORMATION(HttpStatus.BAD_REQUEST, "MEMBER4002", "일치하는 값이 없습니다."),
    NICK_IS_DUPLICATE(HttpStatus.CONFLICT, "MEMBER40901", "이미 존재하는 닉네임입니다."),
    NICK_NAME_NOT_EXIST(HttpStatus.NOT_FOUND, "MEMBER40401", "닉네임이 존재하지 않습니다."),
    MEMBER_DEVICE_TOKEN(HttpStatus.NOT_FOUND, "MEMBER40402", "디바이스 토큰이 존재하지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER40404", "해당 회원이 존재하지 않습니다."),
    OAUTH_USER_INFO_FAIL(HttpStatus.UNAUTHORIZED, "AUTH401", "OAuth에서 사용자 정보를 가져오지 못했습니다."),
    OAUTH_TOKEN_FAIL(HttpStatus.UNAUTHORIZED, "MEMBER401", "토큰 DTO로 변경 실패했습니다."),
    MEMBER_NOT_FOUND_BY_EMAIL(HttpStatus.NOT_FOUND, "MEMBER40405", "해당 이메일로 가입된 회원이 존재하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER40902", "이미 가입된 이메일입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}