package com.project.syncly.domain.note.exception;

import com.project.syncly.global.apiPayload.code.BaseErrorCode;
import com.project.syncly.global.apiPayload.exception.CustomException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NoteException extends CustomException {
    private final String customMessage;

    public NoteException(NoteErrorCode errorCode) {
        super(errorCode);
        this.customMessage = null;
    }

    /**
     * 커스텀 메시지와 함께 예외 생성
     *
     * @param errorCode 에러 코드
     * @param customMessage 추가 상세 메시지
     */
    public NoteException(NoteErrorCode errorCode, String customMessage) {
        super(new CustomErrorCode(errorCode, customMessage));
        this.customMessage = customMessage;
    }

    /**
     * 커스텀 메시지를 포함한 에러 코드 래퍼
     */
    @Getter
    private static class CustomErrorCode implements BaseErrorCode {
        private final HttpStatus status;
        private final String code;
        private final String message;

        public CustomErrorCode(NoteErrorCode baseErrorCode, String customMessage) {
            this.status = baseErrorCode.getStatus();
            this.code = baseErrorCode.getCode();
            this.message = baseErrorCode.getMessage() + " - " + customMessage;
        }
    }
}
