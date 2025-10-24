package com.project.syncly.domain.note.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * OT(Operational Transformation) 기반 편집 연산 DTO
 */
@Getter
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EditOperation {

    /**
     * 연산 타입 ("insert" 또는 "delete")
     */
    private final String type;

    /**
     * 연산 시작 위치 (0-based index)
     */
    private final int position;

    /**
     * 영향받는 문자 수
     */
    private final int length;

    /**
     * 삽입될 내용 (insert인 경우만)
     */
    private final String content;

    /**
     * 연산이 기반한 문서 버전
     */
    private final int revision;

    /**
     * 연산 수행 WorkspaceMember ID
     */
    private final Long workspaceMemberId;

    /**
     * 연산 타임스탬프
     */
    private final LocalDateTime timestamp;

    @JsonCreator
    public EditOperation(
            @JsonProperty("type") String type,
            @JsonProperty("position") int position,
            @JsonProperty("length") int length,
            @JsonProperty("content") String content,
            @JsonProperty("revision") int revision,
            @JsonProperty("workspaceMemberId") Long workspaceMemberId,
            @JsonProperty("timestamp") LocalDateTime timestamp
    ) {
        this.type = type;
        this.position = position;
        this.length = length;
        this.content = content;
        this.revision = revision;
        this.workspaceMemberId = workspaceMemberId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    /**
     * Insert 연산 생성
     */
    public static EditOperation insert(int position, String content, int revision, Long workspaceMemberId) {
        return EditOperation.builder()
                .type("insert")
                .position(position)
                .length(content.length())
                .content(content)
                .revision(revision)
                .workspaceMemberId(workspaceMemberId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Delete 연산 생성
     */
    public static EditOperation delete(int position, int length, int revision, Long workspaceMemberId) {
        return EditOperation.builder()
                .type("delete")
                .position(position)
                .length(length)
                .content(null)
                .revision(revision)
                .workspaceMemberId(workspaceMemberId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Insert 연산인지 확인
     */
    public boolean isInsert() {
        return "insert".equalsIgnoreCase(type);
    }

    /**
     * Delete 연산인지 확인
     */
    public boolean isDelete() {
        return "delete".equalsIgnoreCase(type);
    }

    /**
     * 연산의 끝 위치 계산 (delete 연산용)
     *
     * @return position + length
     */
    public int getEndPosition() {
        return position + length;
    }

    /**
     * 새로운 position으로 연산 복사 (OT 변환용)
     *
     * @param newPosition 새로운 위치
     * @return position이 변경된 새 EditOperation
     */
    public EditOperation withPosition(int newPosition) {
        return EditOperation.builder()
                .type(this.type)
                .position(newPosition)
                .length(this.length)
                .content(this.content)
                .revision(this.revision)
                .workspaceMemberId(this.workspaceMemberId)
                .timestamp(this.timestamp)
                .build();
    }

    /**
     * 새로운 length로 연산 복사 (OT 변환용)
     *
     * @param newLength 새로운 길이
     * @return length가 변경된 새 EditOperation
     */
    public EditOperation withLength(int newLength) {
        return EditOperation.builder()
                .type(this.type)
                .position(this.position)
                .length(newLength)
                .content(this.content)
                .revision(this.revision)
                .workspaceMemberId(this.workspaceMemberId)
                .timestamp(this.timestamp)
                .build();
    }

    /**
     * 새로운 position과 length로 연산 복사 (OT 변환용)
     *
     * @param newPosition 새로운 위치
     * @param newLength 새로운 길이
     * @return position과 length가 변경된 새 EditOperation
     */
    public EditOperation withPositionAndLength(int newPosition, int newLength) {
        return EditOperation.builder()
                .type(this.type)
                .position(newPosition)
                .length(newLength)
                .content(this.content)
                .revision(this.revision)
                .workspaceMemberId(this.workspaceMemberId)
                .timestamp(this.timestamp)
                .build();
    }

    /**
     * No-op (아무것도 하지 않는) 연산으로 변환
     * Delete 연산의 length를 0으로 설정
     *
     * @return length=0인 새 EditOperation
     */
    public EditOperation toNoOp() {
        return this.withLength(0);
    }

    /**
     * 이 연산이 no-op인지 확인
     * (delete 연산이면서 length가 0인 경우)
     *
     * @return no-op 여부
     */
    public boolean isNoOp() {
        return isDelete() && length == 0;
    }

    @Override
    public String toString() {
        if (isInsert()) {
            return String.format("Insert(pos=%d, content='%s', rev=%d, wmId=%d)",
                    position, content, revision, workspaceMemberId);
        } else {
            return String.format("Delete(pos=%d, len=%d, rev=%d, wmId=%d)",
                    position, length, revision, workspaceMemberId);
        }
    }
}
