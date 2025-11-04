package com.project.syncly.domain.note.service;

import com.project.syncly.domain.note.dto.CursorPosition;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Yjs CRDT 기반 실시간 협업 편집 서비스
 *
 * <p><b>아키텍처:</b>
 * <ul>
 *   <li>프론트엔드: Yjs로 CRDT 기반 동시편집 처리 (자동 충돌 해결)</li>
 *   <li>백엔드: Yjs Update 바이너리를 수신하여 Redis에 저장 및 브로드캐스트</li>
 *   <li>클라이언트 간: Update 바이너리 직접 전송 (서버는 중계 역할만)</li>
 * </ul>
 *
 * <p><b>주요 특징:</b>
 * <ul>
 *   <li>OT 변환 불필요 (CRDT가 자동으로 충돌 해결)</li>
 *   <li>Revision 관리 불필요 (Logical Clock으로 자동 처리)</li>
 *   <li>구현 단순화 (바이너리 저장만)</li>
 *   <li>네트워크 지연에 강함 (순서 상관없이 최종 결과 동일)</li>
 * </ul>
 *
 * <p><b>Yjs Update 흐름:</b>
 * <pre>
 * 1. 클라이언트 A: Y.Doc 편집
 *    → Y.encodeStateAsUpdate() → Uint8Array
 *    → Base64 인코딩 → WebSocket 전송
 *
 * 2. 백엔드: Update 수신
 *    → applyYjsUpdate(noteId, base64Update)
 *    → Redis NOTE:YDOC:{noteId}에 저장
 *    → dirty 플래그 설정
 *    → 다른 클라이언트에게 브로드캐스트
 *
 * 3. 클라이언트 B, C: Update 수신
 *    → Y.applyUpdate(ydoc, binaryUpdate)
 *    → 자동 병합 (CRDT 특성)
 *    → 충돌 해결됨 (추가 로직 불필요)
 * </pre>
 *
 * <p><b>VS OT(Operational Transformation):</b>
 * <table border="1">
 *   <tr>
 *     <th>항목</th>
 *     <th>OT</th>
 *     <th>Yjs CRDT</th>
 *   </tr>
 *   <tr>
 *     <td>충돌 해결</td>
 *     <td>수동 transform() 필요</td>
 *     <td>자동 (CRDT 특성)</td>
 *   </tr>
 *   <tr>
 *     <td>Revision</td>
 *     <td>명시적 관리 필요</td>
 *     <td>자동 (Logical Clock)</td>
 *   </tr>
 *   <tr>
 *     <td>히스토리</td>
 *     <td>최근 100개만 (메모리 절약)</td>
 *     <td>무한 히스토리 (CRDT 특성)</td>
 *   </tr>
 *   <tr>
 *     <td>구현 복잡도</td>
 *     <td>높음 (500+ 줄 OTEngine)</td>
 *     <td>낮음 (간단한 저장만)</td>
 *   </tr>
 *   <tr>
 *     <td>네트워크 순서</td>
 *     <td>순서 중요 (OT 변환 필요)</td>
 *     <td>순서 무관 (자동 병합)</td>
 *   </tr>
 * </table>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YjsService {

    private final NoteRedisService noteRedisService;

    /**
     * Yjs Update를 처리하고 Redis에 저장합니다 (State Vector 포함).
     *
     * <p><b>처리 단계:</b>
     * <ol>
     *   <li>Update 유효성 검증</li>
     *   <li>State Vector가 제공되면 저장 (선택적 동기화용)</li>
     *   <li>Redis에 Update 저장 (NOTE:YDOC:{noteId})</li>
     *   <li>dirty 플래그 설정 (자동 저장용)</li>
     *   <li>처리 결과 반환</li>
     * </ol>
     *
     * <p><b>Yjs 표준 프로토콜:</b>
     * <ul>
     *   <li>base64Update: Y.encodeStateAsUpdate() 결과</li>
     *   <li>base64StateVector: 클라이언트의 State Vector (선택적)</li>
     * </ul>
     *
     * <p><b>특징:</b>
     * <ul>
     *   <li>OT 변환 없음 (CRDT가 처리)</li>
     *   <li>단순 저장만 수행</li>
     *   <li>충돌 해결 불필요</li>
     *   <li>State Vector는 향후 선택적 동기화용</li>
     * </ul>
     *
     * @param noteId 노트 ID
     * @param base64Update Base64 인코딩된 Yjs Update
     * @param base64StateVector 클라이언트의 State Vector (선택적, null 가능)
     * @return 처리 결과
     * @throws NoteException Update가 유효하지 않은 경우
     */
    public ApplyYjsUpdateResult applyYjsUpdate(Long noteId, String base64Update, String base64StateVector) {
        log.debug("Yjs Update 처리 시작: noteId={}, updateSize={}", noteId,
                base64Update != null ? base64Update.length() : 0);

        // 1. 유효성 검증
        if (base64Update == null || base64Update.trim().isEmpty()) {
            log.warn("빈 Update 수신: noteId={}", noteId);
            throw new NoteException(NoteErrorCode.INVALID_OPERATION, "Update 데이터가 비어있습니다");
        }

        try {
            // Base64 디코딩하여 유효성 확인 (선택적)
            // byte[] binaryUpdate = Base64.getDecoder().decode(base64Update);
            // log.debug("Update 바이너리 크기: {} bytes", binaryUpdate.length);
        } catch (IllegalArgumentException e) {
            log.error("Base64 디코딩 실패: noteId={}, error={}", noteId, e.getMessage());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION, "유효하지 않은 Update 형식입니다");
        }

        // 2. State Vector가 제공되면 저장 (선택적 동기화용)
        if (base64StateVector != null && !base64StateVector.isEmpty()) {
            try {
                noteRedisService.setStateVector(noteId, base64StateVector);
                log.debug("클라이언트 State Vector 저장: noteId={}, stateVectorSize={}",
                        noteId, base64StateVector.length());
            } catch (Exception e) {
                log.warn("State Vector 저장 실패 (계속 진행): noteId={}, error={}",
                        noteId, e.getMessage());
                // State Vector 저장 실패는 Update 저장을 막지 않음
            }
        }

        // 3. Redis에 Update 저장 (기존 Update와 병합하여 누적, dirty 플래그 자동 설정됨)
        noteRedisService.applyYjsUpdate(noteId, base64Update);
        log.info("Yjs Update 저장 완료: noteId={}, updateSize={}", noteId, base64Update.length());

        // 4. 처리 결과 반환
        return new ApplyYjsUpdateResult(true, base64Update.length());
    }

    /**
     * Yjs Update를 처리하고 Redis에 저장합니다 (State Vector 없음 - 호환성용).
     *
     * <p>State Vector가 필요 없는 경우 이 메서드를 사용합니다.
     * 예: 초기 Y.Doc 로드, DB에서 restore 등
     *
     * @param noteId 노트 ID
     * @param base64Update Base64 인코딩된 Yjs Update
     * @return 처리 결과
     * @throws NoteException Update가 유효하지 않은 경우
     */
    public ApplyYjsUpdateResult applyYjsUpdate(Long noteId, String base64Update) {
        return applyYjsUpdate(noteId, base64Update, null);
    }

    /**
     * Redis에 저장된 Y.Doc 상태를 Update 형식으로 반환합니다.
     *
     * <p><b>사용 시나리오:</b>
     * <pre>
     * // 새 사용자가 노트에 입장했을 때 기존 상태 전송
     * String base64Update = yjsService.getYdocAsUpdate(noteId);
     * if (base64Update != null) {
     *     // 클라이언트에게 Update 전송
     *     wsService.sendInitialState(clientId, base64Update);
     * } else {
     *     // 새 노트인 경우 빈 상태 초기화
     *     yjsService.initializeEmptyDoc(noteId);
     * }
     * </pre>
     *
     * <p><b>주의:</b>
     * <ul>
     *   <li>반환된 Update는 클라이언트가 Y.applyUpdate()로 처리해야 함</li>
     *   <li>여러 Update를 연결한 경우, 클라이언트가 순차적으로 applyUpdate 처리</li>
     * </ul>
     *
     * @param noteId 노트 ID
     * @return Base64 인코딩된 Y.Doc Update (없으면 null)
     */
    public String getYdocAsUpdate(Long noteId) {
        log.debug("Y.Doc 조회: noteId={}", noteId);

        String base64Update = noteRedisService.getYdocBinary(noteId);
        if (base64Update != null && !base64Update.isEmpty()) {
            log.debug("Y.Doc 반환: noteId={}, size={}", noteId, base64Update.length());
            return base64Update;
        }

        log.debug("Y.Doc이 없음: noteId={} (새 노트이거나 초기화 필요)", noteId);
        return null;
    }

    /**
     * 여러 Yjs Update를 병합하여 저장합니다. (선택적 기능)
     *
     * <p><b>구현 전략:</b>
     * <ul>
     *   <li>간단한 병합: Update를 이어붙임 (클라이언트가 순차적으로 처리)</li>
     *   <li>고급 병합: 여러 Update를 하나로 인코딩 (구현 복잡)</li>
     * </ul>
     *
     * <p><b>현재 구현:</b>
     * <ul>
     *   <li>간단한 이어붙임 방식 (delimiter: "|")</li>
     *   <li>클라이언트가 각 Update를 분리하여 applyUpdate 처리</li>
     * </ul>
     *
     * <p><b>사용 예시:</b>
     * <pre>{@code
     * List<String> updates = List.of(
     *     "SGVsbG8gV29ybGQ=",  // "Hello World"
     *     "Zm9vYmFy"            // "foobar"
     * );
     * yjsService.mergeUpdates(noteId, updates);
     * }</pre>
     *
     * @param noteId 노트 ID
     * @param updates 병합할 Update 리스트
     */
    public void mergeUpdates(Long noteId, java.util.List<String> updates) {
        if (updates == null || updates.isEmpty()) {
            log.debug("병합할 Update가 없음: noteId={}", noteId);
            return;
        }

        log.debug("Update 병합 시작: noteId={}, count={}", noteId, updates.size());

        // 간단한 병합: Update를 이어붙임
        String mergedBase64 = String.join("|", updates);
        noteRedisService.setYdocBinary(noteId, mergedBase64);

        log.info("Update 병합 완료: noteId={}, mergedSize={}", noteId, mergedBase64.length());
    }

    /**
     * DB에서 기존 Y.Doc을 Redis로 로드하여 초기화합니다 (dirty 플래그 미설정).
     *
     * <p><b>특징:</b>
     * <ul>
     *   <li>DB에서 로드한 기존 상태를 Redis에 저장</li>
     *   <li>dirty 플래그를 설정하지 않음 (기존 데이터이므로)</li>
     *   <li>이후 사용자 편집으로 인한 Update만 dirty=true로 설정됨</li>
     * </ul>
     *
     * <p><b>사용 시나리오:</b>
     * <pre>
     * // 사용자가 노트에 입장했을 때, Redis에 없으면 DB에서 로드
     * String dbYdoc = note.getYdocBinary();
     * if (dbYdoc != null && !dbYdoc.isEmpty()) {
     *     yjsService.loadFromDatabase(noteId, dbYdoc);  // dirty 플래그 미설정
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     * @param dbYdoc DB에서 로드한 Y.Doc 바이너리
     */
    public void loadFromDatabase(Long noteId, String dbYdoc) {
        log.info("DB에서 Y.Doc 로드: noteId={}, size={}", noteId, dbYdoc != null ? dbYdoc.length() : 0);
        noteRedisService.setYdocBinaryWithoutDirty(noteId, dbYdoc);
    }

    /**
     * 빈 Y.Doc으로 노트를 초기화합니다.
     *
     * <p><b>주의:</b> dirty 플래그를 설정하지 않으므로 자동 저장되지 않습니다.
     *
     * <p><b>사용 시나리오:</b>
     * <pre>
     * // 새 노트 생성 시
     * yjsService.initializeEmptyDoc(noteId);
     *
     * // 또는 기존 노트 초기화
     * String dbContent = note.getYdocBinary();
     * if (dbContent == null) {
     *     yjsService.initializeEmptyDoc(noteId);
     * } else {
     *     yjsService.loadFromDatabase(noteId, dbContent);
     * }
     * </pre>
     *
     * @param noteId 노트 ID
     */
    public void initializeEmptyDoc(Long noteId) {
        log.info("빈 Y.Doc 초기화: noteId={}", noteId);
        // ⚠️ 중요: setYdocBinaryWithoutDirty()로 호출하여 dirty 플래그 미설정
        noteRedisService.setYdocBinaryWithoutDirty(noteId, "");
    }

    /**
     * ℹ️ 커서 관리는 Yjs Awareness API가 전담합니다.
     *
     * <p><b>더 이상 사용되지 않음:</b>
     * <ul>
     *   <li>updateCursor() - Redis에 커서 저장 불필요</li>
     *   <li>getAllCursors() - Awareness가 모든 커서 동기화</li>
     *   <li>removeCursor() - 사용자 퇴장 시 자동 처리</li>
     * </ul>
     *
     * <p><b>Yjs Awareness 흐름:</b>
     * 1. 클라이언트: awareness.setLocalState()로 커서 정보 설정
     * 2. WebsocketProvider: Awareness 변경을 자동으로 다른 클라이언트에게 전송
     * 3. 클라이언트들: awareness 변경 이벤트를 구독하여 커서 동기화
     *
     * <p>이전 OT 기반 편집에서는 서버가 커서를 Redis에 저장했지만,
     * Yjs CRDT로 마이그레이션하면서 모든 커서 관리를 클라이언트 Awareness에서 처리합니다.
     */

    /**
     * Yjs Update 적용 결과 DTO
     *
     * @param success 처리 성공 여부
     * @param updateSize Update 바이너리 크기
     */
    public record ApplyYjsUpdateResult(
            boolean success,
            int updateSize
    ) {}
}
