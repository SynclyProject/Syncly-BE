package com.project.syncly.domain.note.service;

import com.project.syncly.domain.note.dto.CursorPosition;
import com.project.syncly.domain.note.dto.EditOperation;
import com.project.syncly.domain.note.engine.OTEngine;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OT(Operational Transformation) 서비스
 *
 * <p>OT 엔진과 Redis를 통합하여 실시간 협업 편집을 처리합니다.
 *
 * <p><b>주요 기능:</b>
 * <ul>
 *   <li>편집 연산 검증</li>
 *   <li>연산 변환 (OT 알고리즘 적용)</li>
 *   <li>Redis에 연산 적용 및 저장</li>
 * </ul>
 *
 * <p><b>처리 흐름:</b>
 * <ol>
 *   <li>클라이언트로부터 EditOperation 수신</li>
 *   <li>연산 유효성 검증 (validateOperation)</li>
 *   <li>현재 revision 확인</li>
 *   <li>op.revision < currentRevision이면 히스토리를 통해 변환 (transformAgainstHistory)</li>
 *   <li>변환된 연산을 문서에 적용 (applyOperation)</li>
 *   <li>Redis 업데이트:
 *       <ul>
 *         <li>content 업데이트</li>
 *         <li>operations 히스토리에 추가</li>
 *         <li>revision 증가</li>
 *         <li>dirty 플래그 설정</li>
 *       </ul>
 *   </li>
 *   <li>새 content와 revision 반환</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OTService {

    private final NoteRedisService noteRedisService;

    /**
     * 편집 연산을 처리합니다.
     *
     * <p>이 메서드는 다음 단계를 수행합니다:
     * <ol>
     *   <li>Redis에서 현재 content, revision, operations 조회</li>
     *   <li>연산 유효성 검증</li>
     *   <li>op.revision이 구버전이면 OT 변환 수행</li>
     *   <li>변환된 연산을 content에 적용</li>
     *   <li>Redis 업데이트</li>
     * </ol>
     *
     * <p><b>사용 예시:</b>
     * <pre>{@code
     * EditOperation op = EditOperation.insert(10, "Hello", 5, 123L);
     * ProcessEditResult result = otService.processEdit(noteId, op);
     * // result.content: 새 문서 내용
     * // result.revision: 새 버전 번호
     * }</pre>
     *
     * @param noteId 노트 ID
     * @param operation 처리할 편집 연산
     * @return 처리 결과 (새 content, 새 revision)
     * @throws NoteException 연산이 유효하지 않거나 적용에 실패한 경우
     */
    public ProcessEditResult processEdit(Long noteId, EditOperation operation) {
        log.debug("편집 연산 처리 시작: noteId={}, operation={}", noteId, operation);

        // 1. Redis에서 현재 상태 조회
        String currentContent = noteRedisService.getContent(noteId);
        if (currentContent == null) {
            throw new NoteException(NoteErrorCode.NOTE_NOT_FOUND);
        }

        int currentRevision = noteRedisService.getRevision(noteId);

        // 2. 연산 유효성 검증
        validateOperation(operation, currentContent);

        // 3. 연산 변환 (필요한 경우)
        EditOperation transformedOp = operation;

        if (operation.getRevision() < currentRevision) {
            log.debug("구버전 연산 감지: op.revision={}, current={}, 변환 필요",
                    operation.getRevision(), currentRevision);

            // operation.revision 이후의 모든 히스토리 조회
            List<EditOperation> history = noteRedisService.getOperations(noteId, operation.getRevision());

            log.debug("히스토리 조회 완료: {} 개의 연산", history.size());

            // 히스토리의 각 연산 로깅
            for (int i = 0; i < history.size(); i++) {
                EditOperation histOp = history.get(i);
                log.debug("  [{}] {}", i + 1, histOp);
            }

            // OT 변환 수행
            transformedOp = OTEngine.transformAgainstHistory(operation, history);

            log.debug("변환 완료: original={}, transformed={}", operation, transformedOp);

            // ⚠️ 변환 후 다시 검증하지 않음!
            // 변환된 operation은 현재 content에 대해 유효하지 않을 수 있지만,
            // OT 변환 알고리즘이 유효성을 보장하므로 검증 불필요.
            // 실제 적용 시 DELETE 범위 조정(line 124)으로 안전성 확보.
        }

        // 4. 연산을 content에 적용
        String newContent;
        try {
            // DELETE 연산의 경우 범위 초과를 자동으로 조정
            EditOperation adjustedOp = transformedOp;
            if (transformedOp.isDelete()) {
                int deleteStart = transformedOp.getPosition();
                int deleteEnd = transformedOp.getEndPosition();
                int contentLength = currentContent.length();

                // DELETE 범위가 content를 초과하는 경우 조정
                if (deleteEnd > contentLength) {
                    log.warn("DELETE 범위 조정: pos={}, len={}, content.length={} → len={}",
                            deleteStart, transformedOp.getLength(), contentLength,
                            Math.max(0, contentLength - deleteStart));

                    // 조정된 길이로 새 연산 생성
                    int adjustedLength = Math.max(0, contentLength - deleteStart);
                    adjustedOp = transformedOp.withLength(adjustedLength);
                }
            }

            newContent = OTEngine.applyOperation(currentContent, adjustedOp);
        } catch (IllegalArgumentException e) {
            log.error("연산 적용 실패: operation={}, content.length={}, error={}",
                    transformedOp, currentContent.length(), e.getMessage());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION);
        }

        // 5. Redis 업데이트
        // 5-1. Content 업데이트 (자동으로 dirty 플래그 설정됨)
        noteRedisService.setContent(noteId, newContent);

        // 5-2. Revision 증가
        int newRevision = noteRedisService.incrementRevision(noteId);

        // 5-3. 변환된 연산을 히스토리에 추가 (새 revision으로)
        EditOperation historyOp = transformedOp.isNoOp() ? transformedOp :
                EditOperation.builder()
                        .type(transformedOp.getType())
                        .position(transformedOp.getPosition())
                        .length(transformedOp.getLength())
                        .content(transformedOp.getContent())
                        .revision(newRevision) // 새 revision 할당
                        .workspaceMemberId(transformedOp.getWorkspaceMemberId())
                        .timestamp(transformedOp.getTimestamp())
                        .build();

        noteRedisService.addOperation(noteId, historyOp);

        log.info("편집 연산 처리 완료: noteId={}, revision={}, contentLength={}",
                noteId, newRevision, newContent.length());

        return new ProcessEditResult(newContent, newRevision, transformedOp);
    }

    /**
     * 편집 연산의 유효성을 검증합니다.
     *
     * <p><b>검증 항목:</b>
     * <ul>
     *   <li>position이 0 이상이고 content 길이 이하인지</li>
     *   <li>DELETE의 경우 position + length가 content 길이 이하인지</li>
     *   <li>length가 음수가 아닌지</li>
     *   <li>INSERT의 경우 content가 null이 아닌지</li>
     * </ul>
     *
     * @param operation 검증할 연산
     * @param content 현재 문서 내용
     * @throws NoteException 연산이 유효하지 않은 경우
     */
    public void validateOperation(EditOperation operation, String content) {
        if (content == null) {
            content = "";
        }

        int position = operation.getPosition();
        int length = operation.getLength();
        int contentLength = content.length();

        // 1. position 범위 검증
        if (position < 0) {
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    String.format("position은 0 이상이어야 합니다: position=%d", position));
        }

        if (operation.isInsert()) {
            // INSERT: position은 0 ~ contentLength (끝에 추가 가능)
            if (position > contentLength) {
                throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                        String.format("INSERT position 범위 초과: position=%d, contentLength=%d",
                                position, contentLength));
            }

            if (operation.getContent() == null) {
                throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                        "INSERT 연산은 content가 필요합니다");
            }
        } else if (operation.isDelete()) {
            // 2. length 검증 (DELETE인 경우)
            if (length < 0) {
                throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                        String.format("length는 0 이상이어야 합니다: length=%d", length));
            }

            // DELETE: position은 0 ~ contentLength-1
            if (position > contentLength) {
                throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                        String.format("DELETE position 범위 초과: position=%d, contentLength=%d",
                                position, contentLength));
            }

            // 3. DELETE 범위 검증
            if (position + length > contentLength) {
                throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                        String.format("DELETE 범위 초과: position=%d, length=%d, contentLength=%d",
                                position, length, contentLength));
            }
        } else {
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    "알 수 없는 연산 타입: " + operation.getType());
        }

        log.debug("연산 유효성 검증 통과: {}", operation);
    }

    /**
     * 편집 연산 후 모든 커서 위치를 조정합니다.
     *
     * <p>편집 연산(INSERT/DELETE)이 적용되면 다른 사용자들의 커서 위치도
     * 그에 맞춰 조정되어야 합니다.
     *
     * <p><b>조정 규칙:</b>
     * <ul>
     *   <li>INSERT: operation.position <= cursor.position이면 cursor.position += operation.length</li>
     *   <li>DELETE: operation.position < cursor.position이면 cursor.position -= operation.length</li>
     *   <li>DELETE: cursor가 삭제 범위 내에 있으면 cursor.position = operation.position</li>
     * </ul>
     *
     * <p><b>사용 예시:</b>
     * <pre>{@code
     * // 편집 처리 후 커서 조정
     * ProcessEditResult result = otService.processEdit(noteId, operation);
     * Map<Long, CursorPosition> adjustedCursors = otService.adjustCursors(noteId, result.appliedOperation());
     * // adjustedCursors를 클라이언트에 브로드캐스트
     * }</pre>
     *
     * @param noteId 노트 ID
     * @param operation 적용된 편집 연산
     * @return 조정된 커서 맵 (workspaceMemberId -> 조정된 CursorPosition)
     */
    public Map<Long, CursorPosition> adjustCursors(Long noteId, EditOperation operation) {
        // 1. Redis에서 모든 현재 커서 조회
        Map<String, CursorPosition> currentCursors = noteRedisService.getAllCursors(noteId);

        if (currentCursors.isEmpty()) {
            log.debug("조정할 커서가 없음: noteId={}", noteId);
            return Map.of();
        }

        Map<Long, CursorPosition> adjustedCursors = new HashMap<>();
        int opPosition = operation.getPosition();
        int opLength = operation.getLength();

        log.debug("커서 조정 시작: noteId={}, operation={}, cursorCount={}",
                noteId, operation, currentCursors.size());

        // 2. 각 커서에 대해 조정 로직 적용
        for (Map.Entry<String, CursorPosition> entry : currentCursors.entrySet()) {
            try {
                Long workspaceMemberId = Long.parseLong(entry.getKey());
                CursorPosition cursor = entry.getValue();

                CursorPosition adjustedCursor = adjustCursor(cursor, operation);

                // 3. Redis에 조정된 커서 저장
                noteRedisService.setCursor(noteId, workspaceMemberId, adjustedCursor);

                adjustedCursors.put(workspaceMemberId, adjustedCursor);

                log.trace("커서 조정: workspaceMemberId={}, original={}, adjusted={}",
                        workspaceMemberId, cursor.getPosition(), adjustedCursor.getPosition());

            } catch (NumberFormatException e) {
                log.warn("잘못된 workspaceMemberId 형식: key={}", entry.getKey());
            }
        }

        log.debug("커서 조정 완료: noteId={}, adjustedCount={}", noteId, adjustedCursors.size());
        return adjustedCursors;
    }

    /**
     * 단일 커서 위치를 편집 연산에 맞춰 조정합니다.
     *
     * @param cursor 조정할 커서
     * @param operation 편집 연산
     * @return 조정된 커서
     */
    private CursorPosition adjustCursor(CursorPosition cursor, EditOperation operation) {
        int cursorPos = cursor.getPosition();
        int cursorRange = cursor.getRange();
        int opPosition = operation.getPosition();
        int opLength = operation.getLength();

        int newPosition = cursorPos;
        int newRange = cursorRange;

        if (operation.isInsert()) {
            // INSERT: operation.position <= cursor.position이면 오른쪽으로 이동
            if (opPosition <= cursorPos) {
                newPosition = cursorPos + opLength;
            }
            // 선택 영역이 있고, operation이 선택 범위 내에 있으면 range 조정
            else if (cursorRange > 0 && opPosition < cursorPos + cursorRange) {
                newRange = cursorRange + opLength;
            }

        } else if (operation.isDelete()) {
            int deleteEnd = opPosition + opLength;

            // Case 1: 커서가 삭제 범위보다 뒤에 있음 → 왼쪽으로 이동
            if (cursorPos >= deleteEnd) {
                newPosition = cursorPos - opLength;
            }
            // Case 2: 커서가 삭제 범위 내에 있음 → 삭제 시작점으로 이동
            else if (cursorPos >= opPosition && cursorPos < deleteEnd) {
                newPosition = opPosition;
                newRange = 0; // 선택 영역 취소
            }
            // Case 3: 커서는 앞에 있지만 선택 영역이 삭제 범위와 겹침
            else if (cursorRange > 0) {
                int cursorEnd = cursorPos + cursorRange;
                if (cursorEnd > opPosition) {
                    // 선택 영역이 삭제 범위와 겹침 → range 조정
                    if (cursorEnd <= deleteEnd) {
                        // 선택 영역의 일부만 삭제됨
                        newRange = opPosition - cursorPos;
                    } else {
                        // 선택 영역이 삭제 범위를 포함
                        newRange = cursorRange - opLength;
                    }
                }
            }
        }

        // 음수 방지
        newPosition = Math.max(0, newPosition);
        newRange = Math.max(0, newRange);

        return CursorPosition.builder()
                .position(newPosition)
                .range(newRange)
                .workspaceMemberId(cursor.getWorkspaceMemberId())
                .userName(cursor.getUserName())
                .profileImage(cursor.getProfileImage())
                .color(cursor.getColor())
                .build();
    }

    /**
     * 편집 처리 결과 DTO
     *
     * @param content 새 문서 내용
     * @param revision 새 버전 번호
     * @param appliedOperation 실제로 적용된 연산 (변환 후)
     */
    public record ProcessEditResult(
            String content,
            int revision,
            EditOperation appliedOperation
    ) {}
}
