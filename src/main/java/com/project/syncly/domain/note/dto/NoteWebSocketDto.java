package com.project.syncly.domain.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 메시지 전용 DTO 모음
 */
public class NoteWebSocketDto {

    /**
     * 노트 입장 시 입장한 사용자에게만 전송되는 응답 DTO
     *
     * <p>노트의 현재 상태를 모두 포함합니다 (Yjs 기반).
     */
    @Schema(description = "노트 입장 응답 DTO")
    public record EnterResponse(
            @Schema(description = "노트 ID")
            Long noteId,

            @Schema(description = "노트 제목")
            String title,

            @Schema(description = "현재 Y.Doc 상태 (Base64 인코딩된 Yjs Update)")
            String ydocBinary,

            @Schema(description = "현재 활성 사용자 목록")
            List<ActiveUserInfo> activeUsers,

            @Schema(description = "입장한 현재 사용자의 WorkspaceMember ID (Yjs Awareness 초기화용)")
            Long currentUserWorkspaceMemberId,

            @Schema(description = "입장 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 사용자가 노트에 입장했을 때 다른 참여자들에게 브로드캐스트되는 메시지
     */
    @Schema(description = "사용자 입장 알림 DTO")
    public record UserJoinedMessage(
            @Schema(description = "입장한 사용자의 WorkspaceMember ID")
            Long workspaceMemberId,

            @Schema(description = "입장한 사용자 이름 (WorkspaceMember.name)")
            String userName,

            @Schema(description = "입장한 사용자 프로필 이미지 (WorkspaceMember.profileImage)")
            String profileImage,

            @Schema(description = "현재 활성 사용자 수")
            Integer activeUserCount,

            @Schema(description = "입장 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 사용자가 노트를 퇴장했을 때 다른 참여자들에게 브로드캐스트되는 메시지
     */
    @Schema(description = "사용자 퇴장 알림 DTO")
    public record UserLeftMessage(
            @Schema(description = "퇴장한 사용자의 WorkspaceMember ID")
            Long workspaceMemberId,

            @Schema(description = "퇴장한 사용자 이름 (WorkspaceMember.name)")
            String userName,

            @Schema(description = "현재 활성 사용자 수")
            Integer activeUserCount,

            @Schema(description = "퇴장 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 퇴장 성공 응답 (퇴장한 사용자에게 전송)
     */
    @Schema(description = "노트 퇴장 응답 DTO")
    public record LeaveResponse(
            @Schema(description = "노트 ID")
            Long noteId,

            @Schema(description = "메시지")
            String message,

            @Schema(description = "퇴장 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 사용자 정보 DTO (입장 시 활성 사용자 목록 전달용)
     */
    @Schema(description = "활성 사용자 정보 DTO")
    public record ActiveUserInfo(
            @Schema(description = "WorkspaceMember ID")
            Long workspaceMemberId,

            @Schema(description = "사용자 이름 (WorkspaceMember.name)")
            String userName,

            @Schema(description = "프로필 이미지 (WorkspaceMember.profileImage)")
            String profileImage,

            @Schema(description = "사용자 고유 색상 (hex)")
            String color
    ) {}

    // ========== 실시간 편집 관련 DTO (Yjs CRDT 기반) ==========

    /**
     * Yjs Update 요청 DTO (클라이언트 → 서버)
     *
     * <p>Yjs 표준 프로토콜 기반:
     * <ul>
     *   <li>base64Update: Y.encodeStateAsUpdate() 결과를 Base64로 인코딩</li>
     *   <li>base64StateVector: 클라이언트의 State Vector (선택적)</li>
     * </ul>
     *
     * <p>State Vector가 포함되면 서버는 클라이언트가 아직 가지지 않은 Update만 전송할 수 있습니다.
     * CRDT의 특성에 따라 서버는 자동으로 충돌을 해결하므로 추가 변환 로직이 불필요합니다.
     */
    @Schema(description = "Yjs Update 요청 DTO")
    public record YjsUpdateRequest(
            @Schema(description = "Base64 인코딩된 Yjs Update (Y.encodeStateAsUpdate() 결과)")
            String base64Update,

            @Schema(description = "클라이언트의 State Vector (Base64, 선택적) - 필요한 Update만 요청할 때 사용")
            String base64StateVector
    ) {}

    /**
     * Yjs Update 브로드캐스트 메시지 (서버 → 모든 참여자)
     *
     * <p>한 사용자의 편집으로부터 생성된 Yjs Update가 다른 참여자들에게 전파될 때 사용됩니다.
     * CRDT는 Update 수신 순서에 관계없이 모든 클라이언트가 동일한 최종 상태에 도달합니다.
     */
    @Schema(description = "Yjs Update 브로드캐스트 메시지")
    public record YjsUpdateBroadcastMessage(
            @Schema(description = "Base64 인코딩된 Yjs Update (바이너리 형식)")
            String base64Update,

            @Schema(description = "업데이트를 생성한 사용자의 WorkspaceMember ID")
            Long workspaceMemberId,

            @Schema(description = "업데이트한 사용자 이름")
            String userName,

            @Schema(description = "업데이트 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 에러 메시지 DTO (서버 → 특정 사용자)
     *
     * <p>CRDT 기반 동기화: 에러 발생 시 클라이언트가 최신 Y.Doc 상태를 요청하여 동기화합니다.
     * 따라서 에러 메시지에는 Y.Doc 바이너리를 포함하여 클라이언트가 즉시 동기화할 수 있습니다.
     */
    @Schema(description = "에러 메시지 DTO")
    public record ErrorMessage(
            @Schema(description = "에러 코드")
            String code,

            @Schema(description = "에러 메시지")
            String message,

            @Schema(description = "현재 Y.Doc 상태 (Base64, 동기화용)")
            String ydocBinary,

            @Schema(description = "에러 발생 시각")
            LocalDateTime timestamp
    ) {
        /**
         * 동기화 정보 없는 단순 에러 메시지 생성
         */
        public static ErrorMessage of(String code, String message) {
            return new ErrorMessage(code, message, null, LocalDateTime.now());
        }

        /**
         * 동기화 정보 포함 에러 메시지 생성 (Y.Doc 바이너리 포함)
         */
        public static ErrorMessage withSync(String code, String message, String ydocBinary) {
            return new ErrorMessage(code, message, ydocBinary, LocalDateTime.now());
        }
    }

    /**
     * Yjs Update 성공 응답 DTO (서버 → 업데이트 요청한 사용자)
     *
     * <p>CRDT 기반으로는 Update가 자동으로 병합되므로 "적용된 업데이트"라는 개념이 없습니다.
     * 따라서 성공 여부와 Update 크기만 반환합니다.
     */
    @Schema(description = "Yjs Update 성공 응답 DTO")
    public record YjsUpdateResponse(
            @Schema(description = "성공 여부")
            boolean success,

            @Schema(description = "저장된 Update 바이너리 크기 (bytes)")
            int updateSize,

            @Schema(description = "성공 메시지")
            String message,

            @Schema(description = "응답 시각")
            LocalDateTime timestamp
    ) {}

    // ========== 커서 위치 공유 관련 DTO ==========

    /**
     * 커서 업데이트 요청 DTO (클라이언트 → 서버)
     */
    @Schema(description = "커서 업데이트 요청 DTO")
    public record CursorUpdateRequest(
            @Schema(description = "커서 위치 (0-based index)")
            int position,

            @Schema(description = "선택 영역 길이 (0이면 단순 커서)")
            int range
    ) {}

    /**
     * 커서 브로드캐스트 메시지 (서버 → 다른 참여자들)
     */
    @Schema(description = "커서 브로드캐스트 메시지")
    public record CursorBroadcastMessage(
            @Schema(description = "WorkspaceMember ID")
            Long workspaceMemberId,

            @Schema(description = "사용자 이름")
            String userName,

            @Schema(description = "커서 위치 (0-based index)")
            int position,

            @Schema(description = "선택 영역 길이")
            int range,

            @Schema(description = "사용자 고유 색상 (hex)")
            String color,

            @Schema(description = "프로필 이미지")
            String profileImage,

            @Schema(description = "업데이트 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 커서 삭제 메시지 (서버 → 다른 참여자들)
     */
    @Schema(description = "커서 삭제 메시지")
    public record CursorRemovedMessage(
            @Schema(description = "WorkspaceMember ID")
            Long workspaceMemberId,

            @Schema(description = "삭제 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 전체 커서 정보 응답 (입장 시)
     */
    @Schema(description = "전체 커서 정보 응답")
    public record AllCursorsResponse(
            @Schema(description = "모든 활성 커서 (workspaceMemberId -> CursorPosition)")
            Map<Long, CursorPosition> cursors,

            @Schema(description = "응답 시각")
            LocalDateTime timestamp
    ) {}

    /**
     * 커서 조정 브로드캐스트 메시지 (편집 후 커서 위치 자동 조정)
     */
    @Schema(description = "커서 조정 브로드캐스트 메시지")
    public record CursorsAdjustedMessage(
            @Schema(description = "조정된 커서들 (workspaceMemberId -> CursorPosition)")
            Map<Long, CursorPosition> adjustedCursors,

            @Schema(description = "조정 시각")
            LocalDateTime timestamp
    ) {}

    // ========== 자동 저장 관련 DTO ==========

    /**
     * 자동 저장 완료 메시지 (서버 → 모든 참여자)
     *
     * <p>Yjs 기반: Y.Doc 바이너리 전체가 저장되며, 버전 번호가 없습니다.
     * Yjs의 Logical Clock이 자동으로 순서를 관리합니다.
     */
    @Schema(description = "자동 저장 완료 메시지")
    public record SaveCompletedMessage(
            @Schema(description = "저장 시각")
            LocalDateTime savedAt,

            @Schema(description = "메시지", example = "자동 저장됨")
            String message
    ) {
        public static SaveCompletedMessage of() {
            return new SaveCompletedMessage(LocalDateTime.now(), "자동 저장됨");
        }
    }

    // ========== CRUD 기능 관련 DTO ==========

    /**
     * 노트 생성 요청 DTO (클라이언트 → 서버)
     */
    @Schema(description = "노트 생성 요청 DTO")
    public record CreateRequest(
            @Schema(description = "워크스페이스 ID")
            Long workspaceId,

            @Schema(description = "노트 제목")
            String title
    ) {}

    /**
     * 노트 생성 응답 DTO (서버 → 클라이언트)
     */
    @Schema(description = "노트 생성 응답 DTO")
    public record CreateResponse(
            @Schema(description = "생성된 노트 ID")
            Long noteId,

            @Schema(description = "노트 제목")
            String title,

            @Schema(description = "워크스페이스 ID")
            Long workspaceId,

            @Schema(description = "생성자 이름")
            String creatorName,

            @Schema(description = "생성자 프로필 이미지")
            String creatorProfileImage,

            @Schema(description = "생성 시각")
            LocalDateTime createdAt
    ) {}

    /**
     * 노트 목록 조회 요청 DTO (클라이언트 → 서버)
     */
    @Schema(description = "노트 목록 조회 요청 DTO")
    public record ListRequest(
            @Schema(description = "워크스페이스 ID")
            Long workspaceId,

            @Schema(description = "페이지 번호 (0-based)")
            int page,

            @Schema(description = "페이지 크기")
            int size,

            @Schema(description = "정렬 기준 (createdAt, lastModifiedAt 등)")
            String sortBy,

            @Schema(description = "정렬 방향 (asc, desc)")
            String direction
    ) {}

    /**
     * 노트 목록 조회 응답 DTO (서버 → 클라이언트)
     */
    @Schema(description = "노트 목록 조회 응답 DTO")
    public record ListResponse(
            @Schema(description = "노트 목록")
            List<NoteListItem> notes,

            @Schema(description = "전체 노트 개수")
            int totalCount,

            @Schema(description = "현재 페이지")
            int currentPage,

            @Schema(description = "전체 페이지 수")
            int totalPages
    ) {}

    /**
     * 노트 목록 항목 DTO
     */
    @Schema(description = "노트 목록 항목 DTO")
    public record NoteListItem(
            @Schema(description = "노트 ID")
            Long noteId,

            @Schema(description = "노트 제목")
            String title,

            @Schema(description = "생성자 이름")
            String creatorName,

            @Schema(description = "생성자 프로필 이미지")
            String creatorProfileImage,

            @Schema(description = "마지막 수정 시각")
            LocalDateTime lastModifiedAt,

            @Schema(description = "참여자 수")
            int participantCount,

            @Schema(description = "생성 시각")
            LocalDateTime createdAt
    ) {}

    /**
     * 노트 상세 조회 요청 DTO (클라이언트 → 서버)
     */
    @Schema(description = "노트 상세 조회 요청 DTO")
    public record GetDetailRequest(
            @Schema(description = "노트 ID")
            Long noteId
    ) {}

    /**
     * 노트 상세 조회 응답 DTO (서버 → 클라이언트)
     *
     * <p>Yjs 기반: Y.Doc 바이너리와 현재 활성 사용자 정보를 포함합니다.
     */
    @Schema(description = "노트 상세 조회 응답 DTO")
    public record GetDetailResponse(
            @Schema(description = "노트 ID")
            Long noteId,

            @Schema(description = "노트 제목")
            String title,

            @Schema(description = "현재 Y.Doc 상태 (Base64 인코딩된 Yjs Update)")
            String ydocBinary,

            @Schema(description = "워크스페이스 ID")
            Long workspaceId,

            @Schema(description = "생성자 ID")
            Long creatorId,

            @Schema(description = "생성자 이름")
            String creatorName,

            @Schema(description = "생성자 프로필 이미지")
            String creatorProfileImage,

            @Schema(description = "현재 활성 참여자")
            List<ActiveUserInfo> activeParticipants,

            @Schema(description = "마지막 수정 시각")
            LocalDateTime lastModifiedAt,

            @Schema(description = "생성 시각")
            LocalDateTime createdAt
    ) {}

    /**
     * 노트 삭제 요청 DTO (클라이언트 → 서버)
     */
    @Schema(description = "노트 삭제 요청 DTO")
    public record DeleteRequest(
            @Schema(description = "노트 ID")
            Long noteId
    ) {}

    /**
     * 노트 삭제 응답 DTO (서버 → 클라이언트)
     */
    @Schema(description = "노트 삭제 응답 DTO")
    public record DeleteResponse(
            @Schema(description = "삭제 성공 여부")
            boolean success,

            @Schema(description = "응답 메시지")
            String message,

            @Schema(description = "삭제 시각")
            LocalDateTime deletedAt
    ) {}

    /**
     * 노트 수동 저장 응답 DTO (서버 → 모든 참여자)
     *
     * <p>Yjs 기반: Y.Doc 전체가 저장되며, 버전 번호가 제거되었습니다.
     */
    @Schema(description = "노트 수동 저장 응답 DTO")
    public record SaveResponse(
            @Schema(description = "저장된 노트 ID")
            Long noteId,

            @Schema(description = "저장 시각")
            LocalDateTime savedAt,

            @Schema(description = "응답 메시지")
            String message,

            @Schema(description = "저장한 사용자 ID")
            Long savedByWorkspaceMemberId,

            @Schema(description = "저장한 사용자 이름")
            String savedByUserName
    ) {}

    // ========== 실시간 목록 업데이트 관련 DTO ==========

    /**
     * 노트 생성 브로드캐스트 메시지 (서버 → 같은 워크스페이스의 모든 사용자)
     *
     * <p>노트가 생성되었을 때 해당 워크스페이스를 보고 있는 모든 사용자에게 실시간으로 전파됩니다.
     */
    @Schema(description = "노트 생성 브로드캐스트 메시지")
    public record NoteCreatedMessage(
            @Schema(description = "생성된 노트 ID")
            Long noteId,

            @Schema(description = "노트 제목")
            String title,

            @Schema(description = "워크스페이스 ID")
            Long workspaceId,

            @Schema(description = "생성자 이름")
            String creatorName,

            @Schema(description = "생성자 프로필 이미지")
            String creatorProfileImage,

            @Schema(description = "생성 시각")
            LocalDateTime createdAt
    ) {}

    /**
     * 노트 삭제 브로드캐스트 메시지 (서버 → 같은 워크스페이스의 모든 사용자)
     *
     * <p>노트가 삭제되었을 때 해당 워크스페이스를 보고 있는 모든 사용자에게 실시간으로 전파됩니다.
     */
    @Schema(description = "노트 삭제 브로드캐스트 메시지")
    public record NoteDeletedMessage(
            @Schema(description = "삭제된 노트 ID")
            Long noteId,

            @Schema(description = "워크스페이스 ID")
            Long workspaceId,

            @Schema(description = "삭제 시각")
            LocalDateTime deletedAt
    ) {}
}
