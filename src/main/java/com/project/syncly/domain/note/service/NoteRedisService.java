package com.project.syncly.domain.note.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.syncly.domain.note.dto.CursorPosition;
import com.project.syncly.domain.note.dto.EditOperation;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 노트의 실시간 협업 데이터를 Redis에서 관리하는 서비스
 *
 * <p>Redis를 사용하는 이유:
 * <ul>
 *   <li>MySQL: 영구 저장용 (자동 저장 시 최종 상태만 저장)</li>
 *   <li>Redis: 실시간 작업용 (편집 중인 임시 데이터, 빠른 읽기/쓰기)</li>
 * </ul>
 *
 * <p>관리하는 데이터:
 * <ul>
 *   <li>content: 현재 편집 중인 노트 내용 (String)</li>
 *   <li>users: 접속 중인 사용자 목록 (Set)</li>
 *   <li>cursors: 각 사용자의 커서 위치 (Hash)</li>
 *   <li>dirty: 변경사항 있는지 플래그 (Boolean)</li>
 *   <li>revision: 문서 버전 번호 (Integer)</li>
 *   <li>operations: 편집 연산 히스토리 (List, 최근 100개)</li>
 * </ul>
 *
 * <p>TTL (Time To Live):
 * <ul>
 *   <li>모든 키는 24시간 후 자동 삭제</li>
 *   <li>활동이 있을 때마다 TTL 갱신 (refreshTTL)</li>
 *   <li>24시간 동안 아무도 편집 안 하면 자동 정리 → 메모리 절약</li>
 * </ul>
 *
 * @see CursorPosition
 * @see EditOperation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteRedisService {

    private final RedisStorage redisStorage;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    // TTL 설정: 24시간 동안 활동 없으면 자동 삭제
    private static final Duration NOTE_TTL = Duration.ofHours(24);

    // Operation 히스토리 최대 개수: 메모리 절약을 위해 최근 100개만 유지
    private static final int MAX_OPERATIONS_HISTORY = 100;

    // ==================== Content 관리 ====================

    /**
     * 노트 내용 조회
     *
     * <p>Redis Key: NOTE:CONTENT:{noteId}
     *
     * <p>사용 시나리오:
     * <pre>
     * // 사용자가 노트에 입장했을 때
     * String content = redisService.getContent(noteId);
     * if (content == null) {
     *     // Redis에 없으면 DB에서 조회 후 초기화
     *     Note note = noteRepository.findById(noteId);
     *     redisService.initializeNote(noteId, note.getContent());
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @return 노트 내용 (없으면 null)
     */
    public String getContent(Long noteId) {
        String key = RedisKeyPrefix.NOTE_CONTENT.get(noteId);
        String content = redisStorage.getValueAsString(key);
        log.debug("Get note content: noteId={}, exists={}", noteId, content != null);
        return content;
    }

    /**
     * 노트 내용 저장 및 dirty 플래그 자동 설정
     *
     * <p>Redis Key: NOTE:CONTENT:{noteId}
     *
     * <p>중요: 이 메서드를 호출하면 자동으로 dirty 플래그가 true로 설정됩니다.
     * dirty 플래그는 스케줄러가 "DB에 저장해야 할 노트"를 찾는 데 사용됩니다.
     *
     * <p>사용 시나리오:
     * <pre>
     * // 사용자가 편집했을 때
     * String currentContent = redisService.getContent(noteId);
     * String newContent = applyEdit(currentContent, operation);
     * redisService.setContent(noteId, newContent);  // dirty 자동 설정
     *
     * // 30초 후 스케줄러가 자동으로 DB 저장
     * </pre>
     *
     * @param noteId 노트 ID
     * @param content 저장할 내용
     */
    public void setContent(Long noteId, String content) {
        String key = RedisKeyPrefix.NOTE_CONTENT.get(noteId);
        redisStorage.setValueAsString(key, content, NOTE_TTL);
        setDirty(noteId, true);  // 변경사항 있음을 표시
        log.debug("Set note content: noteId={}, length={}", noteId, content != null ? content.length() : 0);
    }

    // ==================== 사용자 관리 (Set) ====================

    /**
     * 접속 중인 사용자 목록 조회
     *
     * <p>Redis Key: NOTE:USERS:{noteId}
     * <p>Redis Type: Set (중복 없는 집합)
     *
     * <p>Set에 저장되는 값: workspaceMemberId (문자열)
     * <p>예: {"123", "456", "789"}
     *
     * <p>사용 시나리오:
     * <pre>
     * // UI에 "3명이 함께 편집 중" 표시
     * Set<String> users = redisService.getActiveUsers(noteId);
     * int count = users.size();  // 3
     *
     * // 각 사용자 정보 조회 (workspaceMemberId → WorkspaceMember)
     * for (String wmId : users) {
     *     WorkspaceMember wm = workspaceMemberRepository.findById(Long.valueOf(wmId));
     *     // wm.getName(), wm.getProfileImage() 사용
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @return 접속 중인 사용자의 workspaceMemberId 집합
     */
    public Set<String> getActiveUsers(Long noteId) {
        String key = RedisKeyPrefix.NOTE_USERS.get(noteId);
        Set<String> users = redisStorage.getSetValues(key);
        log.debug("Get active users: noteId={}, count={}", noteId, users != null ? users.size() : 0);
        return users != null ? users : new HashSet<>();
    }

    /**
     * 사용자 추가 (노트 입장 시)
     *
     * <p>Redis Command: SADD NOTE:USERS:{noteId} {workspaceMemberId}
     *
     * <p>Set 자료구조 특징:
     * <ul>
     *   <li>중복 자동 제거: 같은 사용자가 여러 번 추가해도 한 번만 저장</li>
     *   <li>O(1) 시간 복잡도: 매우 빠른 추가/삭제/조회</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // WebSocket ENTER 메시지 처리
     * {@literal @}MessageMapping("/note.{noteId}.enter")
     * public void handleEnter(Long noteId, Principal principal) {
     *     Long workspaceMemberId = getWorkspaceMemberId(principal);
     *     redisService.addUser(noteId, workspaceMemberId);
     *
     *     Set<String> activeUsers = redisService.getActiveUsers(noteId);
     *     broadcast("user joined", activeUsers);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param workspaceMemberId WorkspaceMember ID (이메일 대신 사용)
     */
    public void addUser(Long noteId, Long workspaceMemberId) {
        String key = RedisKeyPrefix.NOTE_USERS.get(noteId);
        redisStorage.addToSet(key, String.valueOf(workspaceMemberId));
        refreshTTL(key);
        log.info("User added to note: noteId={}, workspaceMemberId={}", noteId, workspaceMemberId);
    }

    /**
     * 사용자 제거 (노트 퇴장 시)
     *
     * <p>Redis Command: SREM NOTE:USERS:{noteId} {workspaceMemberId}
     *
     * <p>사용 시나리오:
     * <pre>
     * // WebSocket LEAVE 메시지 처리
     * {@literal @}MessageMapping("/note.{noteId}.leave")
     * public void handleLeave(Long noteId, Principal principal) {
     *     Long workspaceMemberId = getWorkspaceMemberId(principal);
     *     redisService.removeUser(noteId, workspaceMemberId);
     *
     *     Set<String> activeUsers = redisService.getActiveUsers(noteId);
     *     broadcast("user left", activeUsers);
     * }
     *
     * // WebSocket 연결 끊김 시 자동 처리
     * {@literal @}EventListener
     * public void onDisconnect(SessionDisconnectEvent event) {
     *     // 해당 사용자를 모든 참여 중인 노트에서 제거
     *     redisService.removeUser(noteId, workspaceMemberId);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param workspaceMemberId WorkspaceMember ID
     */
    public void removeUser(Long noteId, Long workspaceMemberId) {
        String key = RedisKeyPrefix.NOTE_USERS.get(noteId);
        redisStorage.removeFromSet(key, String.valueOf(workspaceMemberId));
        log.info("User removed from note: noteId={}, workspaceMemberId={}", noteId, workspaceMemberId);
    }

    // ==================== 커서 관리 (Hash) ====================

    /**
     * 모든 사용자의 커서 정보 조회
     *
     * <p>Redis Key: NOTE:CURSORS:{noteId}
     * <p>Redis Type: Hash (key-value 쌍의 집합)
     *
     * <p>Hash 구조:
     * <pre>
     * NOTE:CURSORS:123 = {
     *   "456": '{"position":10,"range":0,"userName":"홍길동","profileImage":"...","color":"#FF6B6B"}',
     *   "789": '{"position":25,"range":5,"userName":"김철수","profileImage":"...","color":"#4ECDC4"}'
     * }
     * </pre>
     *
     * <p>Hash를 사용하는 이유:
     * <ul>
     *   <li>사용자별로 개별 업데이트 가능 (한 사용자 커서만 업데이트해도 다른 사용자 영향 없음)</li>
     *   <li>HGETALL 명령으로 모든 커서를 한 번에 조회 가능</li>
     *   <li>HDEL 명령으로 특정 사용자 커서만 삭제 가능</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 노트 입장 시 모든 사용자의 커서 위치 가져오기
     * Map<String, CursorPosition> allCursors = redisService.getAllCursors(noteId);
     *
     * // UI에 다른 사용자들의 커서 표시
     * for (CursorPosition cursor : allCursors.values()) {
     *     editor.showRemoteCursor(
     *         cursor.getPosition(),
     *         cursor.getUserName(),
     *         cursor.getColor()
     *     );
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @return Map<workspaceMemberId, CursorPosition>
     */
    public Map<String, CursorPosition> getAllCursors(Long noteId) {
        String key = RedisKeyPrefix.NOTE_CURSORS.get(noteId);
        Map<String, Object> hashEntries = redisStorage.getHash(key);

        if (hashEntries == null || hashEntries.isEmpty()) {
            return new HashMap<>();
        }

        // Hash에서 가져온 JSON 문자열을 CursorPosition 객체로 변환
        Map<String, CursorPosition> cursors = new HashMap<>();
        for (Map.Entry<String, Object> entry : hashEntries.entrySet()) {
            try {
                String json = entry.getValue().toString();
                CursorPosition cursor = redisObjectMapper.readValue(json, CursorPosition.class);
                cursors.put(entry.getKey(), cursor);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cursor position: workspaceMemberId={}", entry.getKey(), e);
            }
        }

        log.debug("Get all cursors: noteId={}, count={}", noteId, cursors.size());
        return cursors;
    }

    /**
     * 커서 위치 저장
     *
     * <p>Redis Command: HSET NOTE:CURSORS:{noteId} {workspaceMemberId} {JSON}
     *
     * <p>JSON 직렬화:
     * <ul>
     *   <li>CursorPosition 객체 → JSON 문자열로 변환</li>
     *   <li>Jackson ObjectMapper 사용 (LocalDateTime 자동 처리)</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // WebSocket CURSOR 메시지 처리
     * {@literal @}MessageMapping("/note.{noteId}.cursor")
     * public void handleCursor(Long noteId, CursorUpdateRequest request) {
     *     Long workspaceMemberId = getCurrentWorkspaceMemberId();
     *     WorkspaceMember wm = workspaceMemberRepository.findById(workspaceMemberId);
     *
     *     CursorPosition cursor = CursorPosition.builder()
     *         .position(request.position())      // 클라이언트가 보낸 위치
     *         .range(request.range())            // 선택 영역 길이
     *         .workspaceMemberId(wm.getId())
     *         .userName(wm.getName())            // WorkspaceMember에서 이름
     *         .profileImage(wm.getProfileImage())
     *         .color(assignColor(wm.getId()))
     *         .build();
     *
     *     redisService.setCursor(noteId, workspaceMemberId, cursor);
     *
     *     // 다른 사용자들에게 브로드캐스트
     *     messagingTemplate.convertAndSend("/topic/note." + noteId, cursor);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param workspaceMemberId WorkspaceMember ID
     * @param cursor 커서 위치 정보 (position, range, userName, profileImage, color 포함)
     */
    public void setCursor(Long noteId, Long workspaceMemberId, CursorPosition cursor) {
        String key = RedisKeyPrefix.NOTE_CURSORS.get(noteId);
        try {
            String json = redisObjectMapper.writeValueAsString(cursor);
            redisStorage.updateHashField(key, String.valueOf(workspaceMemberId), json);
            refreshTTL(key);
            log.debug("Set cursor: noteId={}, workspaceMemberId={}, position={}", noteId, workspaceMemberId, cursor.getPosition());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cursor position", e);
        }
    }

    /**
     * 커서 정보 삭제 (퇴장 시)
     *
     * <p>Redis Command: HDEL NOTE:CURSORS:{noteId} {workspaceMemberId}
     *
     * <p>사용 시나리오:
     * <pre>
     * // 사용자 퇴장 시 커서도 함께 제거
     * {@literal @}MessageMapping("/note.{noteId}.leave")
     * public void handleLeave(Long noteId, Principal principal) {
     *     Long workspaceMemberId = getWorkspaceMemberId(principal);
     *
     *     redisService.removeUser(noteId, workspaceMemberId);
     *     redisService.removeCursor(noteId, workspaceMemberId);  // 커서도 제거
     *
     *     // 다른 사용자 화면에서 커서 사라짐
     *     broadcast("cursor removed", workspaceMemberId);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param workspaceMemberId WorkspaceMember ID
     */
    public void removeCursor(Long noteId, Long workspaceMemberId) {
        String key = RedisKeyPrefix.NOTE_CURSORS.get(noteId);
        redisTemplate.opsForHash().delete(key, String.valueOf(workspaceMemberId));
        log.debug("Remove cursor: noteId={}, workspaceMemberId={}", noteId, workspaceMemberId);
    }

    // ==================== Dirty 플래그 (자동 저장용) ====================

    /**
     * dirty 플래그 확인
     *
     * <p>Redis Key: NOTE:DIRTY:{noteId}
     * <p>Redis Value: "true" 또는 "false" (문자열)
     *
     * <p>dirty 플래그란?
     * <ul>
     *   <li>true: Redis에 변경사항이 있어서 DB에 저장 필요</li>
     *   <li>false: 변경사항 없거나 이미 DB에 저장됨</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 스케줄러(10단계)가 30초마다 실행
     * {@literal @}Scheduled(fixedDelay = 30000)
     * public void autoSave() {
     *     Set<Long> dirtyNoteIds = redisService.getAllDirtyNoteIds();
     *
     *     for (Long noteId : dirtyNoteIds) {
     *         if (redisService.isDirty(noteId)) {
     *             String content = redisService.getContent(noteId);
     *             noteRepository.updateContent(noteId, content);  // DB 저장
     *             redisService.clearDirty(noteId);  // 플래그 초기화
     *         }
     *     }
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @return true: 변경사항 있음, false: 변경사항 없음
     */
    public boolean isDirty(Long noteId) {
        String key = RedisKeyPrefix.NOTE_DIRTY.get(noteId);
        String value = redisStorage.getValueAsString(key);
        return "true".equals(value);
    }

    /**
     * dirty 플래그 설정
     *
     * <p>주의: 이 메서드는 보통 직접 호출하지 않습니다.
     * setContent() 메서드가 자동으로 호출합니다.
     *
     * @param noteId 노트 ID
     * @param dirty true: 변경사항 있음, false: 변경사항 없음
     */
    public void setDirty(Long noteId, boolean dirty) {
        String key = RedisKeyPrefix.NOTE_DIRTY.get(noteId);
        redisStorage.setValueAsString(key, String.valueOf(dirty), NOTE_TTL);
        log.debug("Set dirty flag: noteId={}, dirty={}", noteId, dirty);
    }

    /**
     * dirty 플래그 초기화 (DB 저장 완료 후)
     *
     * <p>사용 시나리오:
     * <pre>
     * // 스케줄러가 DB 저장 후 호출
     * String content = redisService.getContent(noteId);
     * noteRepository.updateContent(noteId, content);  // DB 저장
     * redisService.clearDirty(noteId);  // 저장 완료 표시
     * </pre>
     *
     * @param noteId 노트 ID
     */
    public void clearDirty(Long noteId) {
        setDirty(noteId, false);
    }

    // ==================== Revision 관리 (OT 동시성 제어) ====================

    /**
     * 현재 revision 조회
     *
     * <p>Redis Key: NOTE:REVISION:{noteId}
     * <p>Redis Value: 정수 (문자열로 저장)
     *
     * <p>Revision이란?
     * <ul>
     *   <li>문서의 버전 번호</li>
     *   <li>편집 연산이 적용될 때마다 1씩 증가</li>
     *   <li>OT 알고리즘에서 충돌 감지에 사용</li>
     * </ul>
     *
     * <p>예시:
     * <pre>
     * 초기 상태: revision = 0, content = ""
     * 사용자 A가 "Hello" 삽입 → revision = 1
     * 사용자 B가 " World" 삽입 → revision = 2
     * </pre>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 사용자가 편집 연산 전송 시
     * int clientRevision = editOperation.getRevision();  // 5 (클라이언트가 본 버전)
     * int serverRevision = redisService.getRevision(noteId);  // 7 (현재 서버 버전)
     *
     * if (clientRevision < serverRevision) {
     *     // 중간에 다른 연산이 있었음! Transform 필요
     *     List<EditOperation> missedOps = redisService.getOperations(noteId, clientRevision);
     *     editOperation = OTEngine.transform(editOperation, missedOps);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @return 현재 revision (없으면 0)
     */
    public int getRevision(Long noteId) {
        String key = RedisKeyPrefix.NOTE_REVISION.get(noteId);
        String value = redisStorage.getValueAsString(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.error("Invalid revision format: noteId={}, value={}", noteId, value);
            return 0;
        }
    }

    /**
     * revision 증가 후 반환 (원자적 연산)
     *
     * <p>Redis Command: INCR NOTE:REVISION:{noteId}
     *
     * <p>원자적 연산이란?
     * <ul>
     *   <li>여러 사용자가 동시에 호출해도 안전</li>
     *   <li>Redis의 INCR 명령은 단일 스레드로 실행되어 경쟁 조건 없음</li>
     *   <li>중복된 revision 번호가 절대 발생하지 않음</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 편집 연산 적용 후 버전 증가
     * String currentContent = redisService.getContent(noteId);
     * String newContent = OTEngine.apply(currentContent, operation);
     * redisService.setContent(noteId, newContent);
     *
     * int newRevision = redisService.incrementRevision(noteId);  // 원자적으로 증가
     *
     * // 다른 사용자들에게 브로드캐스트
     * broadcast("content updated", newRevision);
     * </pre>
     *
     * @param noteId 노트 ID
     * @return 증가된 새 revision 번호
     */
    public int incrementRevision(Long noteId) {
        String key = RedisKeyPrefix.NOTE_REVISION.get(noteId);
        Long newRevision = redisTemplate.opsForValue().increment(key);
        refreshTTL(key);
        log.debug("Increment revision: noteId={}, newRevision={}", noteId, newRevision);
        return newRevision != null ? newRevision.intValue() : 1;
    }

    // ==================== Operation 히스토리 (OT용) ====================

    /**
     * operations 리스트에 추가 (최근 100개만 유지)
     *
     * <p>Redis Key: NOTE:OPERATIONS:{noteId}
     * <p>Redis Type: List (순서가 있는 리스트)
     *
     * <p>List 구조:
     * <pre>
     * NOTE:OPERATIONS:123 = [
     *   '{"type":"insert","position":5,"revision":5,...}',   // 가장 오래된 연산
     *   '{"type":"delete","position":10,"revision":6,...}',
     *   ...
     *   '{"type":"insert","position":20,"revision":104,...}' // 가장 최근 연산
     * ]
     * </pre>
     *
     * <p>최근 100개만 유지하는 이유:
     * <ul>
     *   <li>메모리 절약: 무한정 쌓이면 메모리 부족</li>
     *   <li>충분한 히스토리: 대부분의 충돌은 최근 몇 개 연산으로 해결</li>
     *   <li>오래된 연산은 이미 DB에 저장되어 복구 가능</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 편집 연산 적용 후 히스토리에 추가
     * EditOperation operation = EditOperation.insert(5, "Hello", 42, workspaceMemberId);
     * redisService.addOperation(noteId, operation);
     *
     * // 나중에 다른 사용자가 충돌 해결 시 사용
     * List<EditOperation> history = redisService.getOperations(noteId, 40);
     * </pre>
     *
     * @param noteId 노트 ID
     * @param operation 추가할 편집 연산
     */
    public void addOperation(Long noteId, EditOperation operation) {
        String key = RedisKeyPrefix.NOTE_OPERATIONS.get(noteId);
        try {
            String json = redisObjectMapper.writeValueAsString(operation);
            redisTemplate.opsForList().rightPush(key, json);  // 리스트 맨 뒤에 추가

            // 최근 100개만 유지 (LTRIM 명령)
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_OPERATIONS_HISTORY) {
                // 예: size=150이면 앞의 50개 삭제, 뒤의 100개만 남김
                redisTemplate.opsForList().trim(key, size - MAX_OPERATIONS_HISTORY, -1);
            }

            refreshTTL(key);
            log.debug("Add operation: noteId={}, type={}, position={}", noteId, operation.getType(), operation.getPosition());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize edit operation", e);
        }
    }

    /**
     * 특정 revision 이후의 operations 조회
     *
     * <p>OT Transform에서 사용:
     * <ul>
     *   <li>클라이언트가 revision 5를 기준으로 편집했는데</li>
     *   <li>서버는 이미 revision 8이면</li>
     *   <li>revision 6, 7, 8 연산을 가져와서 클라이언트 연산과 transform</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 클라이언트가 보낸 연산
     * EditOperation clientOp = ...; // revision = 5
     * int serverRevision = redisService.getRevision(noteId); // 8
     *
     * if (clientOp.getRevision() < serverRevision) {
     *     // revision 6, 7, 8 연산 가져오기
     *     List<EditOperation> missedOps = redisService.getOperations(noteId, 6);
     *
     *     // Transform (6단계에서 구현)
     *     for (EditOperation missedOp : missedOps) {
     *         clientOp = OTEngine.transform(clientOp, missedOp);
     *     }
     *
     *     // 변환된 연산 적용
     *     String content = OTEngine.apply(currentContent, clientOp);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param fromRevision 이 revision 이후의 연산만 조회 (inclusive)
     * @return 필터링된 연산 리스트 (시간순 정렬)
     */
    public List<EditOperation> getOperations(Long noteId, int fromRevision) {
        String key = RedisKeyPrefix.NOTE_OPERATIONS.get(noteId);
        List<Object> operations = redisTemplate.opsForList().range(key, 0, -1);  // 전체 조회

        if (operations == null || operations.isEmpty()) {
            return new ArrayList<>();
        }

        return operations.stream()
                .map(obj -> {
                    try {
                        String json = obj.toString();
                        return redisObjectMapper.readValue(json, EditOperation.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize edit operation", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(op -> op.getRevision() >= fromRevision)  // fromRevision 이후만 필터링
                .collect(Collectors.toList());
    }

    // ==================== 초기화 및 삭제 ====================

    /**
     * 노트 최초 진입 시 Redis 초기화
     *
     * <p>초기화하는 데이터:
     * <ul>
     *   <li>content: DB에서 가져온 내용</li>
     *   <li>revision: 0으로 시작</li>
     *   <li>dirty: false (변경사항 없음)</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 첫 사용자가 노트 입장 시
     * {@literal @}MessageMapping("/note.{noteId}.enter")
     * public void handleEnter(Long noteId) {
     *     String content = redisService.getContent(noteId);
     *
     *     if (content == null) {
     *         // Redis에 없으면 DB에서 로드 후 초기화
     *         Note note = noteRepository.findById(noteId).orElseThrow();
     *         redisService.initializeNote(noteId, note.getContent());
     *     }
     *
     *     // 이후 사용자들은 Redis에서 바로 조회
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param content 초기 내용 (DB에서 가져온 값, null이면 빈 문자열)
     */
    public void initializeNote(Long noteId, String content) {
        log.info("Initialize note in Redis: noteId={}", noteId);

        // content 초기화
        setContent(noteId, content != null ? content : "");

        // revision 초기화 (0부터 시작)
        String revisionKey = RedisKeyPrefix.NOTE_REVISION.get(noteId);
        redisStorage.setValueAsString(revisionKey, "0", NOTE_TTL);

        // dirty 플래그 초기화 (변경사항 없음)
        clearDirty(noteId);

        log.info("Note initialized successfully: noteId={}", noteId);
    }

    /**
     * 노트 관련 모든 Redis 데이터 삭제
     *
     * <p>삭제되는 데이터:
     * <ul>
     *   <li>content: 노트 내용</li>
     *   <li>users: 접속 중인 사용자</li>
     *   <li>cursors: 커서 위치</li>
     *   <li>dirty: dirty 플래그</li>
     *   <li>revision: 버전 번호</li>
     *   <li>operations: 연산 히스토리</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 노트 삭제 시
     * {@literal @}Transactional
     * public void deleteNote(Long noteId) {
     *     noteRepository.deleteById(noteId);  // DB 삭제
     *     redisService.deleteNoteData(noteId);  // Redis 정리
     * }
     *
     * // 또는 마지막 사용자가 퇴장하고 24시간 후 TTL로 자동 삭제
     * </pre>
     *
     * @param noteId 노트 ID
     */
    public void deleteNoteData(Long noteId) {
        log.info("Delete note data from Redis: noteId={}", noteId);

        redisStorage.delete(RedisKeyPrefix.NOTE_CONTENT.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_USERS.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_CURSORS.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_DIRTY.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_REVISION.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_OPERATIONS.get(noteId));

        log.info("Note data deleted successfully: noteId={}", noteId);
    }

    // ==================== 유틸리티 ====================

    /**
     * TTL 갱신 (활동 있을 때마다)
     *
     * <p>TTL (Time To Live):
     * <ul>
     *   <li>Redis 키가 자동으로 삭제되는 시간</li>
     *   <li>이 메서드를 호출하면 24시간으로 리셋</li>
     *   <li>활동이 없으면 24시간 후 자동 삭제 → 메모리 절약</li>
     * </ul>
     *
     * <p>예시:
     * <pre>
     * 10:00 - 노트 생성, TTL = 24시간 (다음날 10:00에 삭제 예정)
     * 14:00 - 사용자가 편집, refreshTTL 호출 → TTL 리셋 (다음날 14:00에 삭제 예정)
     * 16:00 - 또 편집, refreshTTL 호출 → TTL 리셋 (다음날 16:00에 삭제 예정)
     * ...
     * 24시간 동안 아무도 편집 안 함 → 자동 삭제
     * </pre>
     *
     * @param key Redis 키
     */
    private void refreshTTL(String key) {
        redisTemplate.expire(key, NOTE_TTL);
    }

    /**
     * 모든 dirty 노트 ID 조회 (스케줄러용)
     *
     * <p>사용 시나리오:
     * <pre>
     * // 스케줄러가 30초마다 실행
     * {@literal @}Scheduled(fixedDelay = 30000)
     * public void autoSave() {
     *     Set<Long> dirtyNoteIds = redisService.getAllDirtyNoteIds();
     *     log.info("Found {} dirty notes to save", dirtyNoteIds.size());
     *
     *     for (Long noteId : dirtyNoteIds) {
     *         try {
     *             String content = redisService.getContent(noteId);
     *             noteRepository.updateContent(noteId, content);
     *             redisService.clearDirty(noteId);
     *             log.info("Auto-saved note: {}", noteId);
     *         } catch (Exception e) {
     *             log.error("Failed to save note: {}", noteId, e);
     *         }
     *     }
     * }
     * </pre>
     *
     * @return dirty 플래그가 true인 노트 ID 집합
     */
    public Set<Long> getAllDirtyNoteIds() {
        // Redis SCAN 명령 사용 (KEYS 대신 - non-blocking)
        Set<Long> dirtyNoteIds = new HashSet<>();
        String pattern = RedisKeyPrefix.NOTE_DIRTY.get("*");

        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions()
                                .match(pattern)
                                .count(100) // 한 번에 100개씩 스캔
                                .build()
                );

                while (cursor.hasNext()) {
                    String key = new String(cursor.next());
                    String value = redisStorage.getValueAsString(key);

                    if ("true".equals(value)) {
                        // "NOTE:DIRTY:123" → 123 추출
                        String noteIdStr = key.substring(RedisKeyPrefix.NOTE_DIRTY.get().length());
                        try {
                            dirtyNoteIds.add(Long.parseLong(noteIdStr));
                        } catch (NumberFormatException e) {
                            log.error("Invalid noteId in dirty key: {}", key);
                        }
                    }
                }

                cursor.close();
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to scan dirty note IDs", e);
        }

        log.debug("Found {} dirty notes", dirtyNoteIds.size());
        return dirtyNoteIds;
    }
}
