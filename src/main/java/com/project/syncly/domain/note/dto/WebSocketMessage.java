package com.project.syncly.domain.note.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지를 감싸는 제네릭 래퍼 클래스
 *
 * <p>실시간 협업 노트에서 주고받는 모든 WebSocket 메시지의 공통 형식을 정의합니다.
 *
 * <p><b>메시지 타입 (type):</b>
 * <ul>
 *   <li>ENTER: 노트에 입장 (payload: null)</li>
 *   <li>LEAVE: 노트에서 퇴장 (payload: null)</li>
 *   <li>EDIT: 편집 연산 (payload: EditOperation)</li>
 *   <li>CURSOR: 커서 위치 변경 (payload: CursorPosition)</li>
 *   <li>SAVE: 자동 저장 완료 알림 (payload: SaveResult)</li>
 *   <li>ERROR: 에러 발생 (payload: ErrorDetails)</li>
 * </ul>
 *
 * <p><b>사용 예시:</b>
 * <pre>{@code
 * // 편집 연산 메시지 생성
 * EditOperation operation = EditOperation.insert(10, "Hello", 5, 123L);
 * WebSocketMessage<EditOperation> message = WebSocketMessage.of(
 *     WebSocketMessageType.EDIT,
 *     operation,
 *     123L
 * );
 *
 * // 커서 위치 메시지 생성
 * CursorPosition cursor = CursorPosition.builder()
 *     .position(10)
 *     .range(0)
 *     .workspaceMemberId(123L)
 *     .build();
 * WebSocketMessage<CursorPosition> cursorMsg = WebSocketMessage.of(
 *     WebSocketMessageType.CURSOR,
 *     cursor,
 *     123L
 * );
 * }</pre>
 *
 * @param <T> 메시지 payload의 타입 (EditOperation, CursorPosition 등)
 */
@Getter
@Builder
public class WebSocketMessage<T> {

    /**
     * 메시지 타입 (ENTER, LEAVE, EDIT, CURSOR, SAVE, ERROR)
     */
    private final WebSocketMessageType type;

    /**
     * 메시지 실제 데이터 (타입에 따라 다름)
     */
    private final T payload;

    /**
     * 메시지 발신자의 WorkspaceMember ID
     */
    private final Long workspaceMemberId;

    /**
     * 메시지 전송 시각
     */
    private final LocalDateTime timestamp;

    @JsonCreator
    public WebSocketMessage(
            @JsonProperty("type") WebSocketMessageType type,
            @JsonProperty("payload") T payload,
            @JsonProperty("workspaceMemberId") Long workspaceMemberId,
            @JsonProperty("timestamp") LocalDateTime timestamp
    ) {
        this.type = type;
        this.payload = payload;
        this.workspaceMemberId = workspaceMemberId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    /**
     * WebSocketMessage 생성 헬퍼 메서드
     *
     * @param type 메시지 타입
     * @param payload 메시지 페이로드
     * @param workspaceMemberId 발신자 WorkspaceMember ID
     * @param <T> payload 타입
     * @return 생성된 WebSocketMessage
     */
    public static <T> WebSocketMessage<T> of(
            WebSocketMessageType type,
            T payload,
            Long workspaceMemberId
    ) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .payload(payload)
                .workspaceMemberId(workspaceMemberId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * payload 없는 간단한 메시지 생성 (ENTER, LEAVE 등)
     *
     * @param type 메시지 타입
     * @param workspaceMemberId 발신자 WorkspaceMember ID
     * @return 생성된 WebSocketMessage (payload = null)
     */
    public static WebSocketMessage<Void> ofEmpty(
            WebSocketMessageType type,
            Long workspaceMemberId
    ) {
        return WebSocketMessage.<Void>builder()
                .type(type)
                .payload(null)
                .workspaceMemberId(workspaceMemberId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 에러 메시지 생성
     *
     * @param errorMessage 에러 메시지
     * @param workspaceMemberId 발신자 WorkspaceMember ID
     * @return ERROR 타입의 WebSocketMessage
     */
    public static WebSocketMessage<String> error(String errorMessage, Long workspaceMemberId) {
        return WebSocketMessage.<String>builder()
                .type(WebSocketMessageType.ERROR)
                .payload(errorMessage)
                .workspaceMemberId(workspaceMemberId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
