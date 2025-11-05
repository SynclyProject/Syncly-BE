package com.project.syncly.domain.note.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.syncly.domain.note.dto.CursorPosition;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * 노트의 실시간 협업 데이터를 Redis에서 관리하는 서비스 (Yjs CRDT 기반)
 *
 * <p>아키텍처:
 * <ul>
 *   <li>프론트엔드: Yjs로 CRDT 기반 동시편집 처리 (자동 충돌 해결)</li>
 *   <li>백엔드: Yjs Update 바이너리를 Redis에 저장 및 브로드캐스트 (중계 역할)</li>
 *   <li>MySQL: 영구 저장용 (자동 저장 시 최종 Y.Doc 상태만 저장)</li>
 * </ul>
 *
 * <p>데이터 구조:
 * <ul>
 *   <li>ydoc: Y.Doc을 encodeStateAsUpdate()로 직렬화한 바이너리 (Base64 String)</li>
 *   <li>stateVector: 상태 추적용 벡터 (Base64 String)</li>
 *   <li>users: 접속 중인 사용자 목록 (Set)</li>
 *   <li>cursors: 각 사용자의 커서 위치 (Hash) - Awareness 사용</li>
 *   <li>dirty: 변경사항 있는지 플래그 (Boolean, 자동 저장용)</li>
 * </ul>
 *
 * <p>Yjs Update 흐름:
 * <pre>
 * 1. 클라이언트 A: Y.Doc 편집 → Y.encodeStateAsUpdate() → Base64 → WebSocket 전송
 * 2. 백엔드: WebSocket 수신 → Base64 디코딩 → Redis 저장 (NOTE:YDOC:{noteId})
 * 3. 브로드캐스트: 다른 클라이언트에게 Update 전송
 * 4. 클라이언트 B, C: Y.applyUpdate() → 자동 병합 (CRDT 특성)
 * </pre>
 *
 * <p>TTL (Time To Live):
 * <ul>
 *   <li>모든 키는 24시간 후 자동 삭제</li>
 *   <li>활동이 있을 때마다 TTL 갱신 (refreshTTL)</li>
 *   <li>24시간 동안 아무도 편집 안 하면 자동 정리 → 메모리 절약</li>
 * </ul>
 *
 * @see CursorPosition
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

    // ==================== Yjs Y.Doc 관리 (CRDT 기반) ====================

    /**
     * Y.Doc 상태 조회 (Yjs Update 바이너리)
     *
     * <p>Redis Key: NOTE:YDOC:{noteId}
     * <p>Redis Value: Base64 encoded Yjs Update (바이너리)
     *
     * <p>사용 시나리오:
     * <pre>
     * // 새 사용자가 노트 입장 시
     * String base64Update = redisService.getYdocBinary(noteId);
     * if (base64Update == null) {
     *     // Redis에 없으면 DB에서 조회 후 초기화
     *     Note note = noteRepository.findById(noteId);
     *     redisService.initializeNote(noteId, note.getYdocBinary());
     * }
     * // 클라이언트에게 base64Update 전송 → applyUpdate()로 동기화
     * </pre>
     *
     * @param noteId 노트 ID
     * @return Base64 encoded Yjs Update (없으면 null)
     */
    public String getYdocBinary(Long noteId) {
        String key = RedisKeyPrefix.NOTE_YDOC.get(noteId);
        String base64Update = redisStorage.getValueAsString(key);
        log.debug("Get Y.Doc: noteId={}, exists={}, size={}", noteId, base64Update != null,
                base64Update != null ? base64Update.length() : 0);
        return base64Update;
    }

    /**
     * Y.Doc 상태 저장 (Yjs Update 바이너리) 및 dirty 플래그 자동 설정
     *
     * <p>Redis Key: NOTE:YDOC:{noteId}
     * <p>Redis Value: Base64 encoded Yjs Update
     *
     * <p>중요: 이 메서드를 호출하면 자동으로 dirty 플래그가 true로 설정됩니다.
     * dirty 플래그는 스케줄러가 "DB에 저장해야 할 노트"를 찾는 데 사용됩니다.
     *
     * <p>사용 시나리오:
     * <pre>
     * // WebSocket에서 Yjs Update 수신
     * String base64Update = wsMessage.getUpdate(); // "SGVsbG8gV29ybGQ="
     * redisService.setYdocBinary(noteId, base64Update);  // dirty 자동 설정
     *
     * // 30초 후 스케줄러가 자동으로 DB에 저장
     * Note note = noteRepository.findById(noteId);
     * note.setYdocBinary(base64Update);
     * noteRepository.save(note);
     * </pre>
     *
     * @param noteId 노트 ID
     * @param base64Update Base64 encoded Yjs Update
     */
    public void setYdocBinary(Long noteId, String base64Update) {
        String key = RedisKeyPrefix.NOTE_YDOC.get(noteId);
        redisStorage.setValueAsString(key, base64Update, NOTE_TTL);
        setDirty(noteId, true);  // 변경사항 있음을 표시
        log.debug("Set Y.Doc: noteId={}, updateSize={}", noteId,
                base64Update != null ? base64Update.length() : 0);
    }

    /**
     * Y.Doc 상태 저장 (dirty 플래그 설정하지 않음 - 초기화 전용)
     *
     * <p>Redis Key: NOTE:YDOC:{noteId}
     * <p>Redis Value: Base64 encoded Yjs Update
     *
     * <p>주의: 이 메서드는 dirty 플래그를 설정하지 않습니다.
     * DB에서 노트를 로드하여 Redis를 초기화할 때만 사용하세요.
     * 사용자 편집으로 인한 Update 저장 시에는 setYdocBinary() 또는 applyYjsUpdate()를 사용하세요.
     *
     * <p>사용 시나리오:
     * <pre>
     * // 첫 사용자가 노트에 입장했을 때, DB에서 기존 상태 로드
     * String dbYdoc = note.getYdocBinary();
     * redisService.setYdocBinaryWithoutDirty(noteId, dbYdoc);
     * // dirty 플래그가 설정되지 않으므로 자동 저장되지 않음
     * </pre>
     *
     * @param noteId 노트 ID
     * @param base64Update Base64 encoded Yjs Update
     */
    public void setYdocBinaryWithoutDirty(Long noteId, String base64Update) {
        String key = RedisKeyPrefix.NOTE_YDOC.get(noteId);
        redisStorage.setValueAsString(key, base64Update, NOTE_TTL);
        log.debug("Set Y.Doc (without dirty flag): noteId={}, updateSize={}", noteId,
                base64Update != null ? base64Update.length() : 0);
    }

    /**
     * Yjs Update 바이너리 머징 (클라이언트가 보낸 Update를 기존 Update와 병합)
     *
     * <p>구현:
     * <ul>
     *   <li>기존 Y.Doc 바이너리 (Base64) 디코딩 → 바이너리로 변환</li>
     *   <li>새로운 Update (Base64) 디코딩 → 바이너리로 변환</li>
     *   <li>바이너리 레벨에서 연결하여 저장 (클라이언트가 applyUpdate 할 때 자동 병합)</li>
     * </ul>
     *
     * <p>중요: 반드시 바이너리 레벨에서 병합해야 함
     * <ul>
     *   <li>✅ 올바른 방법: Base64("AAA") + Base64("BBB") → 바이너리 병합 → Base64 인코딩</li>
     *   <li>❌ 잘못된 방법: Base64("AAA") + "|" + Base64("BBB") → Base64 "AAA|BBB" (유효하지 않은 문자)</li>
     * </ul>
     *
     * <p>참고:
     * <ul>
     *   <li>Yjs Y.applyUpdate()는 여러 바이너리 Update를 순차적으로 처리 가능</li>
     *   <li>바이너리 레벨에서 연결하면 클라이언트가 한 번에 applyUpdate() 호출 가능</li>
     *   <li>Yjs CRDT 특성상 어떤 순서로 applyUpdate해도 최종 결과는 같음</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // WebSocket에서 Yjs Update 수신 (클라이언트 A)
     * String newUpdate = wsMessage.getUpdate();  // "QnluYXJ5RGF0YQ=="
     * redisService.applyYjsUpdate(noteId, newUpdate);
     *
     * // 내부적으로:
     * // 1. 기존 Y.Doc 조회 (Base64): "QnluYXJ5RGF0YTE="
     * // 2. 기존과 새 Update 바이너리 레벨에서 병합
     * // 3. 병합된 바이너리를 Base64 인코딩: "QnluYXJ5RGF0YTFCaW5hcnlEYXRh"
     * // 4. Redis 저장
     * // 5. 다른 클라이언트에게 브로드캐스트
     * </pre>
     *
     * @param noteId 노트 ID
     * @param newUpdateBase64 새로운 Yjs Update (Base64 인코딩된 바이너리)
     */
    public void applyYjsUpdate(Long noteId, String newUpdateBase64) {
        String key = RedisKeyPrefix.NOTE_YDOC.get(noteId);

        // 🔍 [Step 1] 입력값 검증
        log.info("🔍 [Redis Step 1] Update 수신 - noteId={}, newSize={}, newFirst30={}",
                noteId,
                newUpdateBase64 != null ? newUpdateBase64.length() : 0,
                newUpdateBase64 != null ?
                    (newUpdateBase64.length() > 30 ? newUpdateBase64.substring(0, 30) : newUpdateBase64)
                    : "null");

        // ✅ 개선된 방식: Redis에 "누적된 업데이트"를 저장합니다
        // (프론트엔드에서 보낸 incremental update들을 계속 누적)
        //
        // 프론트엔드는 매번 Y.Doc.encodeStateAsUpdate()를 호출하므로,
        // 이것들은 모두 "현재까지의 모든 변경사항을 포함"하고 있습니다.
        // 따라서 가장 최신의 Update만 저장하면 됩니다!

        String currentBase64 = redisStorage.getValueAsString(key);

        // 🔍 [Step 2] 기존 데이터 확인
        if (currentBase64 != null && !currentBase64.isEmpty()) {
            log.info("🔍 [Redis Step 2] 기존 Update 확인 - noteId={}, currentSize={}, currentFirst30={}",
                    noteId,
                    currentBase64.length(),
                    currentBase64.length() > 30 ? currentBase64.substring(0, 30) : currentBase64);
        } else {
            log.info("🔍 [Redis Step 2] 기존 Update 없음 - noteId={}", noteId);
        }

        // ⚠️ 기존 데이터가 있으면, 새 Update와 병합해야 합니다
        // 하지만 단순 연결이 아닌, 프론트의 최신 상태를 신뢰합니다
        String mergedBase64;

        if (currentBase64 != null && !currentBase64.isEmpty()) {
            // ✅ 핵심: 프론트엔드에서 온 Update는 이미 모든 변경사항을 포함하고 있으므로
            // 단순히 새로운 Update로 대체합니다 (가장 안전한 방법)
            mergedBase64 = newUpdateBase64;
            log.info("🔍 [Redis Step 3] 병합 - 새로운 Update로 대체 (기존은 폐기)");
        } else {
            mergedBase64 = newUpdateBase64;
            log.info("🔍 [Redis Step 3] 새로운 Update (기존 없음)");
        }

        try {
            // 🔍 [Step 4] Redis 저장
            log.info("🔍 [Redis Step 4] Redis 저장 시작 - noteId={}, mergedSize={}, mergedFirst30={}",
                    noteId,
                    mergedBase64.length(),
                    mergedBase64.length() > 30 ? mergedBase64.substring(0, 30) : mergedBase64);

            redisStorage.setValueAsString(key, mergedBase64, NOTE_TTL);
            setDirty(noteId, true);
            refreshTTL(key);

            // 🔍 [Step 5] Redis 저장 검증
            String verifyBase64 = redisStorage.getValueAsString(key);
            if (verifyBase64 != null && verifyBase64.equals(mergedBase64)) {
                log.info("✔️ [Redis Step 5] Redis 저장 검증 성공 - noteId={}, verifySize={}, 일치=true",
                        noteId,
                        verifyBase64.length());
            } else {
                log.warn("⚠️ [Redis Step 5] Redis 저장 검증 실패 - 저장한Size={}, 조회한Size={}, 일치=false",
                        mergedBase64.length(),
                        verifyBase64 != null ? verifyBase64.length() : 0);
            }

            log.info("✅ Yjs Update 저장 완료: noteId={}, 최종Size={}", noteId, mergedBase64.length());
        } catch (Exception e) {
            log.error("❌ Yjs Update 저장 실패: noteId={}", noteId, e);
            throw new RuntimeException("Yjs Update 저장 실패", e);
        }
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

    // ==================== State Vector 관리 (Yjs 상태 추적용) ====================

    /**
     * State Vector 조회 (상태 추적용)
     *
     * <p>Redis Key: NOTE:STATE_VECTOR:{noteId}
     * <p>Redis Value: Base64 encoded state vector
     *
     * <p>State Vector란?
     * <ul>
     *   <li>각 클라이언트/서버의 "이 정도까지 봤다"는 상태 정보</li>
     *   <li>클라이언트가 노트에 입장할 때, 서버가 필요한 Update만 선택적으로 전송하기 위해 사용</li>
     *   <li>현재 구현에서는 선택적 기능 (모든 Update를 전송해도 CRDT가 처리함)</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 클라이언트가 노트 입장 시
     * String clientStateVector = wsMessage.getStateVector();  // 클라이언트가 알고 있는 상태
     * String serverStateVector = redisService.getStateVector(noteId);  // 서버의 상태
     *
     * // 필요한 Update만 필터링해서 전송 (현재는 모두 전송)
     * String update = redisService.getYdocBinary(noteId);
     * wsService.sendUpdate(clientId, update);
     * </pre>
     *
     * @param noteId 노트 ID
     * @return Base64 encoded state vector (없으면 null)
     */
    public String getStateVector(Long noteId) {
        String key = RedisKeyPrefix.NOTE_STATE_VECTOR.get(noteId);
        String stateVector = redisStorage.getValueAsString(key);
        log.debug("Get state vector: noteId={}, exists={}", noteId, stateVector != null);
        return stateVector;
    }

    /**
     * State Vector 저장
     *
     * <p>Redis Key: NOTE:STATE_VECTOR:{noteId}
     *
     * <p>현재 구현에서는 선택적 기능입니다.
     * 필요하면 클라이언트가 보낸 state vector를 저장할 수 있습니다.
     *
     * @param noteId 노트 ID
     * @param stateVectorBase64 Base64 encoded state vector
     */
    public void setStateVector(Long noteId, String stateVectorBase64) {
        String key = RedisKeyPrefix.NOTE_STATE_VECTOR.get(noteId);
        redisStorage.setValueAsString(key, stateVectorBase64, NOTE_TTL);
        log.debug("Set state vector: noteId={}", noteId);
    }

    // ==================== 초기화 및 삭제 ====================

    /**
     * 노트 최초 진입 시 Redis 초기화 (Yjs 기반)
     *
     * <p>초기화하는 데이터:
     * <ul>
     *   <li>ydoc: DB에서 가져온 Y.Doc (Base64 encoded)</li>
     *   <li>stateVector: 초기 상태 벡터</li>
     *   <li>dirty: false (변경사항 없음)</li>
     * </ul>
     *
     * <p>사용 시나리오:
     * <pre>
     * // 첫 사용자가 노트 입장 시
     * {@literal @}MessageMapping("/note.{noteId}.enter")
     * public void handleEnter(Long noteId) {
     *     String ydoc = redisService.getYdocBinary(noteId);
     *
     *     if (ydoc == null) {
     *         // Redis에 없으면 DB에서 로드 후 초기화
     *         Note note = noteRepository.findById(noteId).orElseThrow();
     *         redisService.initializeNote(noteId, note.getYdocBinary());
     *     }
     *
     *     // 클라이언트에게 ydoc 전송 → applyUpdate()로 동기화
     *     wsService.sendInitialState(clientId, ydoc);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param ydocBase64 초기 Y.Doc (Base64 encoded, DB에서 가져온 값, null이면 빈 Y.Doc)
     */
    public void initializeNote(Long noteId, String ydocBase64) {
        log.info("Initialize note in Redis: noteId={}", noteId);

        // Y.Doc 초기화 (DB에서 가져온 상태 또는 빈 Y.Doc)
        if (ydocBase64 != null && !ydocBase64.isEmpty()) {
            setYdocBinary(noteId, ydocBase64);
        } else {
            // 빈 Y.Doc으로 초기화 (새 노트인 경우)
            // 프론트엔드에서 생성한 빈 Y.Doc의 Update
            // ⚠️ 중요: 빈 문자열 저장 금지! dirty 플래그 미설정
            setYdocBinaryWithoutDirty(noteId, "");
        }

        // dirty 플래그 초기화 (변경사항 없음)
        clearDirty(noteId);

        log.info("Note initialized successfully: noteId={}", noteId);
    }

    /**
     * 노트 관련 모든 Redis 데이터 삭제 (Yjs 기반)
     *
     * <p>삭제되는 데이터:
     * <ul>
     *   <li>ydoc: Y.Doc 바이너리</li>
     *   <li>stateVector: 상태 벡터</li>
     *   <li>users: 접속 중인 사용자</li>
     *   <li>cursors: 커서 위치</li>
     *   <li>dirty: dirty 플래그</li>
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

        redisStorage.delete(RedisKeyPrefix.NOTE_YDOC.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_STATE_VECTOR.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_USERS.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_CURSORS.get(noteId));
        redisStorage.delete(RedisKeyPrefix.NOTE_DIRTY.get(noteId));

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
