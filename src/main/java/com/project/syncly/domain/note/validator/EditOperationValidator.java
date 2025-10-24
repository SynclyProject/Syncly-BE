package com.project.syncly.domain.note.validator;

import com.project.syncly.domain.note.dto.EditOperation;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 편집 연산 유효성 검증 컴포넌트
 *
 * <p>클라이언트로부터 받은 편집 연산(INSERT/DELETE)을 검증합니다.
 * OT(Operational Transformation) 알고리즘의 안전성을 보장합니다.
 */
@Slf4j
@Component
public class EditOperationValidator {

    /**
     * 편집 연산 전체 유효성 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>연산 타입이 INSERT 또는 DELETE인가?</li>
     *   <li>position이 유효한가? (0 이상)</li>
     *   <li>length가 유효한가? (0 이상)</li>
     *   <li>현재 내용에 대해 연산이 유효한가?</li>
     *   <li>INSERT 시 content가 null이 아닌가?</li>
     *   <li>revision이 유효한가? (0 이상)</li>
     * </ul>
     *
     * @param operation 검증할 편집 연산
     * @param currentContent 현재 노트 내용
     * @throws NoteException 연산이 유효하지 않음
     */
    public void validate(EditOperation operation, String currentContent) {
        if (operation == null) {
            log.warn("null 편집 연산 시도");
            throw new NoteException(NoteErrorCode.INVALID_OPERATION, "연산이 null입니다");
        }

        // 1. 연산 타입 검증
        validateOperationType(operation.getType());

        // 2. 기본 필드 검증
        validateBasicFields(operation);

        // 3. 연산별 상세 검증
        if ("insert".equalsIgnoreCase(operation.getType())) {
            validateInsertOperation(operation, currentContent);
        } else if ("delete".equalsIgnoreCase(operation.getType())) {
            validateDeleteOperation(operation, currentContent);
        }

        // 4. 메타데이터 검증
        validateMetadata(operation);

        log.debug("편집 연산 검증 성공: type={}, position={}, revision={}",
                operation.getType(), operation.getPosition(), operation.getRevision());
    }

    /**
     * 연산 타입 검증
     *
     * @param type 연산 타입 ("insert" 또는 "delete")
     * @throws NoteException 지원하지 않는 연산 타입
     */
    private void validateOperationType(String type) {
        if (type == null) {
            log.warn("null 연산 타입");
            throw new NoteException(NoteErrorCode.INVALID_OPERATION, "연산 타입이 null입니다");
        }

        String lowerType = type.toLowerCase();
        if (!lowerType.equals("insert") && !lowerType.equals("delete")) {
            log.warn("지원하지 않는 연산 타입: type={}", type);
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    String.format("지원하지 않는 연산 타입: %s (insert 또는 delete만 가능)", type));
        }
    }

    /**
     * 기본 필드 검증
     *
     * <p>position과 length의 기본 유효성을 확인합니다.
     *
     * @param operation 편집 연산
     * @throws NoteException position 또는 length가 유효하지 않음
     */
    private void validateBasicFields(EditOperation operation) {
        // position 검증
        if (operation.getPosition() < 0) {
            log.warn("음수 position: position={}", operation.getPosition());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    String.format("position은 0 이상이어야 합니다 (현재: %d)", operation.getPosition()));
        }

        // length 검증 (DELETE 연산에서만 사용)
        if ("delete".equalsIgnoreCase(operation.getType())) {
            if (operation.getLength() <= 0) {
                log.warn("DELETE 연산 길이 검증 실패: length={}", operation.getLength());
                throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                        String.format("DELETE 연산의 length는 1 이상이어야 합니다 (현재: %d)", operation.getLength()));
            }
        }
    }

    /**
     * INSERT 연산 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>삽입할 내용(content)이 null이 아닌가?</li>
     *   <li>삽입 위치가 현재 내용 길이 이하인가?</li>
     * </ul>
     *
     * @param operation INSERT 연산
     * @param currentContent 현재 노트 내용
     * @throws NoteException INSERT 연산이 유효하지 않음
     */
    private void validateInsertOperation(EditOperation operation, String currentContent) {
        // 삽입할 내용 검증
        if (operation.getContent() == null || operation.getContent().isEmpty()) {
            log.warn("INSERT 연산에서 빈 내용: content={}", operation.getContent());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    "INSERT 연산의 content는 비워둘 수 없습니다");
        }

        // 삽입 위치 검증
        int contentLength = currentContent != null ? currentContent.length() : 0;
        if (operation.getPosition() > contentLength) {
            log.warn("INSERT 연산 위치 초과: position={}, contentLength={}",
                    operation.getPosition(), contentLength);
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    String.format("INSERT 위치는 %d 이하여야 합니다 (현재: %d)",
                            contentLength, operation.getPosition()));
        }
    }

    /**
     * DELETE 연산 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>삭제 범위가 현재 내용을 초과하지 않는가?</li>
     *   <li>삭제할 내용이 있는가? (position + length <= content.length())</li>
     * </ul>
     *
     * @param operation DELETE 연산
     * @param currentContent 현재 노트 내용
     * @throws NoteException DELETE 연산이 유효하지 않음
     */
    private void validateDeleteOperation(EditOperation operation, String currentContent) {
        int contentLength = currentContent != null ? currentContent.length() : 0;

        // position + length가 content 범위를 초과하지 않는지 확인
        if (operation.getPosition() + operation.getLength() > contentLength) {
            log.warn("DELETE 연산 범위 초과: position={}, length={}, contentLength={}",
                    operation.getPosition(), operation.getLength(), contentLength);
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    String.format("DELETE 범위가 내용을 초과합니다 (position+length=%d, content length=%d)",
                            operation.getPosition() + operation.getLength(), contentLength));
        }
    }

    /**
     * 메타데이터 검증
     *
     * <p>revision, workspaceMemberId 등을 검증합니다.
     *
     * @param operation 편집 연산
     * @throws NoteException 메타데이터가 유효하지 않음
     */
    private void validateMetadata(EditOperation operation) {
        // revision 검증
        if (operation.getRevision() < 0) {
            log.warn("음수 revision: revision={}", operation.getRevision());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    String.format("revision은 0 이상이어야 합니다 (현재: %d)", operation.getRevision()));
        }

        // workspaceMemberId 검증
        if (operation.getWorkspaceMemberId() == null || operation.getWorkspaceMemberId() <= 0) {
            log.warn("유효하지 않은 workspaceMemberId: workspaceMemberId={}",
                    operation.getWorkspaceMemberId());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION,
                    "workspaceMemberId가 유효하지 않습니다");
        }
    }

    /**
     * 현재 revision과 연산의 revision 비교
     *
     * <p>OT 알고리즘에서 필요한 검증입니다.
     * 클라이언트가 본 revision과 서버의 현재 revision이 일치하지 않으면
     * transformation이 필요합니다.
     *
     * @param operationRevision 연산의 revision
     * @param serverRevision 서버의 현재 revision
     * @throws NoteException revision이 일치하지 않음 (transformation 필요)
     */
    public void validateRevisionMatch(int operationRevision, int serverRevision) {
        if (operationRevision != serverRevision) {
            log.warn("revision 불일치: operationRevision={}, serverRevision={}",
                    operationRevision, serverRevision);
            throw new NoteException(NoteErrorCode.REVISION_MISMATCH);
        }
    }

    /**
     * 편집 작업자의 유효성 검증
     *
     * <p>편집 연산을 수행하는 사용자가 노트에 접근 가능한지 확인합니다.
     *
     * @param operation 편집 연산
     * @param noteCreatorId 노트 작성자 ID
     */
    public void validateEditor(EditOperation operation, Long noteCreatorId) {
        if (operation.getWorkspaceMemberId() == null || operation.getWorkspaceMemberId() <= 0) {
            log.warn("유효하지 않은 편집자: workspaceMemberId={}", operation.getWorkspaceMemberId());
            throw new NoteException(NoteErrorCode.INVALID_OPERATION, "편집자 정보가 유효하지 않습니다");
        }
    }
}
