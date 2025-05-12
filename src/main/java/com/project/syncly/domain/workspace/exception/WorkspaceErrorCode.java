package com.project.syncly.domain.workspace.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WorkspaceErrorCode implements BaseErrorCode {

    // 사용자 관련 에러
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Auth404_0", "존재하지 않는 유저입니다."),
    ALREADY_HAS_PERSONAL_WORKSPACE(HttpStatus.CONFLICT, "Workspace409_0", "개인 워크스페이스는 하나만 생성할 수 있습니다."),

    // 워크스페이스 관련 에러
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace404_0", "워크스페이스가 존재하지 않습니다."),
    NOT_TEAM_WORKSPACE(HttpStatus.BAD_REQUEST, "Workspace400_1", "팀 워크스페이스가 아닙니다."),


    // 초대 관련 에러
    INVITEE_NOT_FOUND(HttpStatus.NOT_FOUND, "Invite404_0", "초대할 사용자를 찾을 수 없습니다."),
    ALREADY_WORKSPACE_MEMBER(HttpStatus.BAD_REQUEST, "Invite400_0", "이미 워크스페이스에 참여 중인 사용자입니다."),
    ALREADY_INVITED(HttpStatus.CONFLICT, "Invite409_0", "이미 초대한 사용자입니다."),
    CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "Invite400_1", "자기 자신은 초대할 수 없습니다."),
    NOT_WORKSPACE_MEMBER(HttpStatus.BAD_REQUEST, "Invite400_2", "워크스페이스의 멤버가 아니므로 초대할 수 없습니다."),

    // 초대 - 메일 발송 관련 에러
    TOKEN_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Invite500_0", "초대 토큰 생성에 반복적으로 실패했습니다."),
    MESSAGE_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Mail500_0", "메일 메시지 생성 중 오류가 발생했습니다."),
    MAIL_SENDING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Mail500_1", "메일 발송 중 오류가 발생했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
