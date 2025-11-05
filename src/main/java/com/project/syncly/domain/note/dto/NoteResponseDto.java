package com.project.syncly.domain.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class NoteResponseDto {

    @Schema(description = "노트 생성 응답 DTO")
    public record Create(
            Long id,
            String title,
            Long workspaceId,
            String creatorName,
            LocalDateTime createdAt
    ) {}

    @Schema(description = "노트 상세 조회 응답 DTO")
    public record Detail(
            Long id,
            String title,
            String content,
            Long workspaceId,
            Long creatorId,
            String creatorName,
            String creatorProfileImage,
            LocalDateTime lastModifiedAt,
            LocalDateTime createdAt,
            Long participantCount,
            List<ParticipantInfo> activeParticipants
    ) {}

    @Schema(description = "노트 목록 조회 응답 DTO")
    public record ListItem(
            Long id,
            String title,
            Long creatorId,
            String creatorName,
            String creatorProfileImage,
            LocalDateTime lastModifiedAt,
            LocalDateTime createdAt,
            Long participantCount
    ) {}

    @Schema(description = "노트 목록 응답 DTO")
    public record NoteList(
            List<ListItem> notes,
            Long totalCount,
            Integer currentPage,
            Integer totalPages
    ) {}

    @Schema(description = "노트 삭제 응답 DTO")
    public record Delete(
            String message
    ) {}

    @Schema(description = "참여자 정보 DTO")
    public record ParticipantInfo(
            Long memberId,
            String memberName,
            String profileImage,
            Boolean isOnline,
            LocalDateTime joinedAt
    ) {}

    @Schema(description = "노트 저장 응답 DTO (Yjs CRDT 기반)")
    public record SaveResponse(
            @Schema(description = "저장 성공 여부")
            boolean success,

            @Schema(description = "저장 시각")
            LocalDateTime savedAt,

            @Schema(description = "메시지")
            String message
    ) {
        public static SaveResponse success(LocalDateTime savedAt) {
            return new SaveResponse(true, savedAt, "노트가 저장되었습니다");
        }

        public static SaveResponse failure(String message) {
            return new SaveResponse(false, LocalDateTime.now(), message);
        }
    }

    @Schema(description = "노트 제목 수정 응답 DTO")
    public record UpdateTitleResponse(
            Long id,
            String title,
            LocalDateTime lastModifiedAt
    ) {}

}
