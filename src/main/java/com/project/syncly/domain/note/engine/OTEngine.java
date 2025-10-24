package com.project.syncly.domain.note.engine;

import com.project.syncly.domain.note.dto.EditOperation;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * OT(Operational Transformation) 엔진
 *
 * <p>동시 편집 충돌을 해결하기 위한 핵심 변환 로직을 제공합니다.
 *
 * <p><b>OT의 원리:</b>
 * <ul>
 *   <li>두 사용자가 동시에 같은 문서의 다른 버전을 편집할 때</li>
 *   <li>나중에 도착한 연산을 먼저 적용된 연산에 맞춰 "변환"합니다</li>
 *   <li>이를 통해 모든 클라이언트가 최종적으로 동일한 상태에 도달합니다</li>
 * </ul>
 *
 * <p><b>예시:</b>
 * <pre>
 * 초기 문서: "Hello"
 *
 * 사용자 A: position 5에 " World" 삽입 → "Hello World"
 * 사용자 B: position 0에서 1자 삭제 → "ello"
 *
 * B의 연산이 먼저 서버에 도착:
 * 1. 서버: "ello" 적용, revision=1
 * 2. A의 연산(rev=0)이 도착하면 B의 연산으로 변환:
 *    - transform(A_insert, B_delete)
 *    - A의 position 5 → 4로 조정 (1자가 삭제되었으므로)
 * 3. 최종: "ello" + " World" at position 4 = "ello World"
 * </pre>
 */
@Slf4j
public class OTEngine {

    /**
     * 두 연산 중 하나를 다른 연산에 맞춰 변환합니다.
     *
     * <p>OT의 핵심 메서드입니다. op가 appliedOp 이후에 적용될 수 있도록
     * op의 position 또는 length를 조정합니다.
     *
     * @param op 변환하려는 연산 (새로 들어온 연산)
     * @param appliedOp 이미 적용된 연산
     * @return 변환된 연산
     */
    public static EditOperation transform(EditOperation op, EditOperation appliedOp) {
        if (op.isInsert() && appliedOp.isInsert()) {
            return transformInsertInsert(op, appliedOp);
        } else if (op.isInsert() && appliedOp.isDelete()) {
            return transformInsertDelete(op, appliedOp);
        } else if (op.isDelete() && appliedOp.isInsert()) {
            return transformDeleteInsert(op, appliedOp);
        } else if (op.isDelete() && appliedOp.isDelete()) {
            return transformDeleteDelete(op, appliedOp);
        }

        // 도달하지 않아야 함
        log.warn("Unknown operation types: op={}, appliedOp={}", op.getType(), appliedOp.getType());
        return op;
    }

    /**
     * INSERT vs INSERT 변환
     *
     * <p><b>시나리오:</b> 두 사용자가 동시에 텍스트를 삽입
     *
     * <p><b>변환 규칙:</b>
     * <ul>
     *   <li>appliedOp.position < op.position: op를 오른쪽으로 이동 (appliedOp.length만큼)</li>
     *   <li>appliedOp.position > op.position: op는 그대로 유지</li>
     *   <li>appliedOp.position == op.position: workspaceMemberId로 우선순위 결정
     *       <ul>
     *         <li>작은 ID가 우선 (왼쪽에 삽입)</li>
     *         <li>큰 ID는 오른쪽으로 이동</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p><b>예시:</b>
     * <pre>
     * 문서: "Hello"
     * appliedOp: Insert "X" at position 2 → "HeXllo"
     * op: Insert "Y" at position 3 → 원래 목표는 "HelYlo"
     *
     * 변환 후 op: Insert "Y" at position 4 (2 + length(X))
     * 최종: "HeXlYlo"
     * </pre>
     *
     * @param op 변환할 INSERT 연산
     * @param appliedOp 이미 적용된 INSERT 연산
     * @return 변환된 INSERT 연산
     */
    private static EditOperation transformInsertInsert(EditOperation op, EditOperation appliedOp) {
        // appliedOp가 op보다 앞에 삽입된 경우
        if (appliedOp.getPosition() < op.getPosition()) {
            // op의 위치를 오른쪽으로 이동 (appliedOp가 삽입한 길이만큼)
            return op.withPosition(op.getPosition() + appliedOp.getLength());
        }
        // 같은 위치에 삽입하는 경우: workspaceMemberId로 우선순위 결정
        else if (appliedOp.getPosition() == op.getPosition()) {
            // workspaceMemberId가 작은 쪽이 왼쪽(먼저) 삽입
            // appliedOp의 ID가 작으면 op를 오른쪽으로 이동
            if (appliedOp.getWorkspaceMemberId() < op.getWorkspaceMemberId()) {
                return op.withPosition(op.getPosition() + appliedOp.getLength());
            }
            // op의 ID가 더 작으면 그대로 유지 (op가 왼쪽에 삽입됨)
            return op;
        }
        // appliedOp가 op보다 뒤에 삽입된 경우: op는 영향받지 않음
        else {
            return op;
        }
    }

    /**
     * INSERT vs DELETE 변환
     *
     * <p><b>시나리오:</b> 한 사용자는 삽입하고, 다른 사용자는 삭제
     *
     * <p><b>변환 규칙:</b>
     * <ul>
     *   <li>appliedOp(DELETE)가 op(INSERT) 앞쪽을 삭제: op를 왼쪽으로 이동</li>
     *   <li>appliedOp가 op 위치를 포함해서 삭제: op를 삭제 시작점으로 이동</li>
     *   <li>appliedOp가 op 뒤쪽을 삭제: op는 영향받지 않음</li>
     * </ul>
     *
     * <p><b>예시:</b>
     * <pre>
     * 문서: "Hello World"
     * appliedOp: Delete 6 chars at position 5 → "Hello" (뒤의 " World" 삭제)
     * op: Insert "!" at position 11 → 원래 목표는 "Hello World!"
     *
     * 변환 후 op: Insert "!" at position 5 (11 - 6)
     * 최종: "Hello!"
     * </pre>
     *
     * @param op 변환할 INSERT 연산
     * @param appliedOp 이미 적용된 DELETE 연산
     * @return 변환된 INSERT 연산
     */
    private static EditOperation transformInsertDelete(EditOperation op, EditOperation appliedOp) {
        int deleteStart = appliedOp.getPosition();
        int deleteEnd = appliedOp.getEndPosition();

        // DELETE가 INSERT보다 완전히 뒤에 있는 경우: INSERT는 영향받지 않음
        if (deleteStart >= op.getPosition()) {
            return op;
        }
        // DELETE가 INSERT 위치를 포함하는 경우: INSERT를 DELETE 시작점으로 이동
        else if (deleteEnd >= op.getPosition()) {
            return op.withPosition(deleteStart);
        }
        // DELETE가 INSERT보다 앞에 있는 경우: INSERT를 왼쪽으로 이동
        else {
            return op.withPosition(op.getPosition() - appliedOp.getLength());
        }
    }

    /**
     * DELETE vs INSERT 변환
     *
     * <p><b>시나리오:</b> 한 사용자는 삭제하고, 다른 사용자는 삽입
     *
     * <p><b>변환 규칙:</b>
     * <ul>
     *   <li>appliedOp(INSERT)가 op(DELETE) 앞에 삽입: op를 오른쪽으로 이동</li>
     *   <li>appliedOp가 op 범위 내에 삽입: op의 길이를 늘림 (삽입된 텍스트도 삭제)</li>
     *   <li>appliedOp가 op 뒤에 삽입: op는 영향받지 않음</li>
     * </ul>
     *
     * <p><b>예시:</b>
     * <pre>
     * 문서: "Hello World"
     * appliedOp: Insert "Beautiful " at position 6 → "Hello Beautiful World"
     * op: Delete 5 chars at position 6 → 원래 목표는 "Hello " (World 삭제)
     *
     * 변환 후 op: Delete 5 chars at position 16 (6 + length("Beautiful "))
     * 최종: "Hello Beautiful "
     * </pre>
     *
     * @param op 변환할 DELETE 연산
     * @param appliedOp 이미 적용된 INSERT 연산
     * @return 변환된 DELETE 연산
     */
    private static EditOperation transformDeleteInsert(EditOperation op, EditOperation appliedOp) {
        int deleteStart = op.getPosition();
        int deleteEnd = op.getEndPosition();

        // INSERT가 DELETE보다 앞에 있는 경우: DELETE를 오른쪽으로 이동
        if (appliedOp.getPosition() < deleteStart) {
            return op.withPosition(op.getPosition() + appliedOp.getLength());
        }
        // INSERT가 DELETE 범위 내에 있는 경우: DELETE 길이를 늘림
        else if (appliedOp.getPosition() >= deleteStart && appliedOp.getPosition() < deleteEnd) {
            // 삽입된 텍스트도 함께 삭제하도록 길이 증가
            return op.withLength(op.getLength() + appliedOp.getLength());
        }
        // INSERT가 DELETE보다 뒤에 있는 경우: DELETE는 영향받지 않음
        else {
            return op;
        }
    }

    /**
     * DELETE vs DELETE 변환
     *
     * <p><b>시나리오:</b> 두 사용자가 동시에 텍스트를 삭제
     *
     * <p><b>변환 규칙:</b>
     * <ul>
     *   <li>두 DELETE가 겹치지 않음: position만 조정</li>
     *   <li>두 DELETE가 부분적으로 겹침: length 조정</li>
     *   <li>appliedOp가 op를 완전히 포함: op를 no-op으로 변환 (length=0)</li>
     * </ul>
     *
     * <p><b>예시 1 - 겹치지 않음:</b>
     * <pre>
     * 문서: "Hello World"
     * appliedOp: Delete 5 chars at position 0 → " World" ("Hello" 삭제)
     * op: Delete 5 chars at position 6 → 원래 목표는 "Hello " ("World" 삭제)
     *
     * 변환 후 op: Delete 5 chars at position 1 (6 - 5)
     * 최종: " " (양쪽 모두 삭제됨)
     * </pre>
     *
     * <p><b>예시 2 - 부분 겹침:</b>
     * <pre>
     * 문서: "Hello World"
     * appliedOp: Delete 3 chars at position 3 → "Hel World" ("lo " 삭제)
     * op: Delete 5 chars at position 5 → 원래 목표는 "Hello" (" World" 삭제)
     *
     * 변환 후 op: Delete 3 chars at position 3 (겹치는 1자는 이미 삭제됨)
     * 최종: "Hel"
     * </pre>
     *
     * <p><b>예시 3 - 완전 포함:</b>
     * <pre>
     * 문서: "Hello World"
     * appliedOp: Delete 11 chars at position 0 → "" (전체 삭제)
     * op: Delete 5 chars at position 6 → 원래 목표는 "Hello " ("World" 삭제)
     *
     * 변환 후 op: Delete 0 chars (no-op, 이미 삭제된 범위)
     * 최종: ""
     * </pre>
     *
     * @param op 변환할 DELETE 연산
     * @param appliedOp 이미 적용된 DELETE 연산
     * @return 변환된 DELETE 연산
     */
    private static EditOperation transformDeleteDelete(EditOperation op, EditOperation appliedOp) {
        int opStart = op.getPosition();
        int opEnd = op.getEndPosition();
        int appliedStart = appliedOp.getPosition();
        int appliedEnd = appliedOp.getEndPosition();

        // Case 1: appliedOp가 op보다 완전히 뒤에 있음 → op는 영향받지 않음
        if (appliedStart >= opEnd) {
            return op;
        }
        // Case 2: appliedOp가 op보다 완전히 앞에 있음 → op의 position만 조정
        else if (appliedEnd <= opStart) {
            return op.withPosition(opStart - appliedOp.getLength());
        }
        // Case 3: appliedOp가 op를 완전히 포함 → op를 no-op으로 변환
        else if (appliedStart <= opStart && appliedEnd >= opEnd) {
            // op가 삭제하려던 범위가 이미 모두 삭제됨
            return op.toNoOp(); // length = 0
        }
        // Case 4: op가 appliedOp를 완전히 포함 → op의 length 감소
        else if (opStart < appliedStart && opEnd > appliedEnd) {
            // op의 중간 부분이 이미 삭제됨
            return op.withLength(op.getLength() - appliedOp.getLength());
        }
        // Case 5: 부분 겹침 - appliedOp가 op의 앞부분과 겹침
        else if (appliedStart <= opStart && appliedEnd < opEnd) {
            // op의 시작 부분이 이미 삭제됨
            int newStart = appliedStart;
            int newLength = opEnd - appliedEnd;
            return op.withPositionAndLength(newStart, newLength);
        }
        // Case 6: 부분 겹침 - appliedOp가 op의 뒷부분과 겹침
        else if (appliedStart > opStart && appliedEnd >= opEnd) {
            // op의 끝 부분이 이미 삭제됨
            int newLength = appliedStart - opStart;
            return op.withLength(newLength);
        }

        // 이론상 도달하지 않아야 함
        log.warn("Unexpected DELETE-DELETE case: op={}, appliedOp={}", op, appliedOp);
        return op;
    }

    /**
     * 연산을 히스토리의 모든 연산에 대해 순차적으로 변환합니다.
     *
     * <p>op.revision 이후에 적용된 모든 연산들에 대해 transform을 반복 적용합니다.
     *
     * <p><b>예시:</b>
     * <pre>
     * op.revision = 5
     * history = [op6, op7, op8, op9] (revision 6~9)
     *
     * 변환 과정:
     * 1. op' = transform(op, op6)
     * 2. op'' = transform(op', op7)
     * 3. op''' = transform(op'', op8)
     * 4. op'''' = transform(op''', op9)
     * 반환: op''''
     * </pre>
     *
     * @param op 변환할 연산
     * @param history 적용된 연산 히스토리 (op.revision 이후의 연산들)
     * @return 모든 히스토리에 대해 변환된 연산
     */
    public static EditOperation transformAgainstHistory(EditOperation op, List<EditOperation> history) {
        EditOperation transformedOp = op;

        for (EditOperation appliedOp : history) {
            // op.revision 이후의 연산들만 변환에 사용
            if (appliedOp.getRevision() > op.getRevision()) {
                transformedOp = transform(transformedOp, appliedOp);

                // no-op이 되면 더 이상 변환할 필요 없음
                if (transformedOp.isNoOp()) {
                    log.debug("Operation became no-op after transform: original={}, appliedOp={}",
                            op, appliedOp);
                    break;
                }
            }
        }

        return transformedOp;
    }

    /**
     * 연산을 문서 내용에 실제로 적용합니다.
     *
     * <p><b>INSERT:</b> content.substring(0, pos) + op.content + content.substring(pos)
     * <p><b>DELETE:</b> content.substring(0, pos) + content.substring(pos + len)
     *
     * @param content 현재 문서 내용
     * @param op 적용할 연산
     * @return 연산이 적용된 새 문서 내용
     * @throws IllegalArgumentException position/length가 범위를 벗어나는 경우
     */
    public static String applyOperation(String content, EditOperation op) {
        if (content == null) {
            content = "";
        }

        // No-op인 경우 아무것도 하지 않음
        if (op.isNoOp()) {
            return content;
        }

        if (op.isInsert()) {
            return applyInsert(content, op);
        } else if (op.isDelete()) {
            return applyDelete(content, op);
        }

        throw new IllegalArgumentException("Unknown operation type: " + op.getType());
    }

    /**
     * INSERT 연산을 문서에 적용
     *
     * @param content 현재 문서 내용
     * @param op INSERT 연산
     * @return 삽입 후 문서 내용
     */
    private static String applyInsert(String content, EditOperation op) {
        int position = op.getPosition();

        // position 범위 검증
        if (position < 0 || position > content.length()) {
            throw new IllegalArgumentException(
                    String.format("Insert position out of bounds: position=%d, contentLength=%d",
                            position, content.length())
            );
        }

        if (op.getContent() == null) {
            throw new IllegalArgumentException("Insert operation must have content");
        }

        // 삽입 수행: 앞부분 + 삽입 내용 + 뒷부분
        String before = content.substring(0, position);
        String after = content.substring(position);
        return before + op.getContent() + after;
    }

    /**
     * DELETE 연산을 문서에 적용
     *
     * @param content 현재 문서 내용
     * @param op DELETE 연산
     * @return 삭제 후 문서 내용
     */
    private static String applyDelete(String content, EditOperation op) {
        int position = op.getPosition();
        int length = op.getLength();

        // position 범위 검증
        if (position < 0 || position > content.length()) {
            throw new IllegalArgumentException(
                    String.format("Delete position out of bounds: position=%d, contentLength=%d",
                            position, content.length())
            );
        }

        // length 범위 검증
        if (length < 0) {
            throw new IllegalArgumentException("Delete length cannot be negative: " + length);
        }

        if (position + length > content.length()) {
            throw new IllegalArgumentException(
                    String.format("Delete range out of bounds: position=%d, length=%d, contentLength=%d",
                            position, length, content.length())
            );
        }

        // 삭제 수행: 앞부분 + 뒷부분 (중간 부분 제거)
        String before = content.substring(0, position);
        String after = content.substring(position + length);
        return before + after;
    }
}
