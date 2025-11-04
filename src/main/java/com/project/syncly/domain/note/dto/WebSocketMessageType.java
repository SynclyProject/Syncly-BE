package com.project.syncly.domain.note.dto;

/**
 * WebSocket 메시지 타입을 정의하는 Enum
 *
 * <p>실시간 협업 노트에서 사용하는 모든 메시지 타입을 정의합니다.
 */
public enum WebSocketMessageType {
    /**
     * 노트 입장
     * - 사용자가 노트에 처음 접속했을 때
     * - payload: null
     * - 서버 → 다른 참여자들에게 브로드캐스트
     */
    ENTER,

    /**
     * 노트 퇴장
     * - 사용자가 노트를 나가거나 연결이 끊겼을 때
     * - payload: null
     * - 서버 → 다른 참여자들에게 브로드캐스트
     */
    LEAVE,

    /**
     * Yjs 업데이트
     * - 사용자가 텍스트를 편집했을 때
     * - payload: Base64 인코딩된 Yjs Update (Yjs CRDT 기반)
     * - 클라이언트 → 서버 → 다른 참여자들에게 브로드캐스트
     * - 참고: OT(Operational Transformation)에서 Yjs CRDT로 마이그레이션
     */
    EDIT,

    /**
     * 커서/선택 범위 동기화
     * - Yjs Awareness API로 자동 동기화됨
     * - payload: CursorPosition (position, range 정보)
     * - Redis 저장 불필요 (Awareness가 전담)
     * - 참고: 사용되지 않음 (Awareness 사용)
     */
    CURSOR,

    /**
     * 자동 저장 완료
     * - 서버에서 Redis 데이터를 DB에 저장 완료했을 때
     * - payload: SaveResult (저장 시각, 버전 정보)
     * - 서버 → 모든 참여자들에게 브로드캐스트
     */
    SAVE,

    /**
     * 에러 발생
     * - 작업 처리 중 에러가 발생했을 때
     * - payload: ErrorDetails 또는 String (에러 메시지)
     * - 서버 → 특정 사용자에게 전송 (유니캐스트)
     */
    ERROR,

    /**
     * 노트 생성
     * - 사용자가 새로운 노트를 생성했을 때
     * - payload: CreateResponse (noteId, title, creatorName, createdAt)
     * - 클라이언트 → 서버 → 응답 전송 (유니캐스트)
     */
    CREATE,

    /**
     * 노트 목록 조회
     * - 사용자가 노트 목록을 요청했을 때
     * - payload: ListResponse (notes, totalCount, currentPage, totalPages)
     * - 클라이언트 → 서버 → 응답 전송 (유니캐스트)
     */
    LIST,

    /**
     * 노트 상세 조회
     * - 사용자가 특정 노트의 상세 정보를 요청했을 때
     * - payload: GetDetailResponse (noteId, title, content, creator, activeParticipants)
     * - 클라이언트 → 서버 → 응답 전송 (유니캐스트)
     */
    GET_DETAIL,

    /**
     * 노트 삭제
     * - 사용자가 노트를 삭제했을 때
     * - payload: DeleteResponse (success, message)
     * - 클라이언트 → 서버 → 응답 전송 (유니캐스트)
     */
    DELETE,

    /**
     * 노트 수동 저장
     * - 사용자가 수동으로 저장 버튼을 클릭했을 때
     * - payload: SaveResponse (revision, savedAt, message)
     * - 클라이언트 → 서버 → 모든 참여자에게 브로드캐스트
     */
    MANUAL_SAVE,

    /**
     * 노트 생성됨 (브로드캐스트)
     * - 새로운 노트가 생성되었을 때
     * - payload: NoteCreatedMessage (noteId, title, creatorName, createdAt)
     * - 서버 → 같은 워크스페이스의 모든 사용자에게 브로드캐스트
     */
    NOTE_CREATED,

    /**
     * 노트 삭제됨 (브로드캐스트)
     * - 노트가 삭제되었을 때
     * - payload: NoteDeletedMessage (noteId, deletedAt)
     * - 서버 → 같은 워크스페이스의 모든 사용자에게 브로드캐스트
     */
    NOTE_DELETED
}
