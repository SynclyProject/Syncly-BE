package com.project.syncly.domain.note.converter;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.note.dto.NoteRequestDto;
import com.project.syncly.domain.note.dto.NoteResponseDto;
import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.entity.NoteParticipant;
import com.project.syncly.domain.workspace.entity.Workspace;

import java.util.List;

public class NoteConverter {

    /**
     * 노트 생성 요청 DTO를 Note 엔티티로 변환
     */
    public static Note toNote(NoteRequestDto.Create dto, Workspace workspace, Member creator) {
        return Note.builder()
                .workspace(workspace)
                .creator(creator)
                .title(dto.title())
                .content("") // 초기 생성 시 빈 내용
                .build();
    }

    /**
     * Note 엔티티를 생성 응답 DTO로 변환
     */
    public static NoteResponseDto.Create toCreateResponse(Note note) {
        return new NoteResponseDto.Create(
                note.getId(),
                note.getTitle(),
                note.getWorkspace().getId(),
                note.getCreator().getName(),
                note.getCreatedAt()
        );
    }

    /**
     * Note 엔티티를 상세 응답 DTO로 변환
     */
    public static NoteResponseDto.Detail toDetailResponse(Note note, List<NoteParticipant> activeParticipants) {
        return new NoteResponseDto.Detail(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getWorkspace().getId(),
                note.getCreator().getId(),
                note.getCreator().getName(),
                note.getCreator().getProfileImage(),
                note.getLastModifiedAt(),
                note.getCreatedAt(),
                (long) activeParticipants.size(),
                activeParticipants.stream()
                        .map(NoteConverter::toParticipantInfo)
                        .toList()
        );
    }

    /**
     * Note 엔티티를 목록 아이템 DTO로 변환
     */
    public static NoteResponseDto.ListItem toListItemResponse(Note note, Long participantCount) {
        return new NoteResponseDto.ListItem(
                note.getId(),
                note.getTitle(),
                note.getCreator().getId(),
                note.getCreator().getName(),
                note.getCreator().getProfileImage(),
                note.getLastModifiedAt(),
                note.getCreatedAt(),
                participantCount
        );
    }

    /**
     * NoteParticipant를 참여자 정보 DTO로 변환
     */
    public static NoteResponseDto.ParticipantInfo toParticipantInfo(NoteParticipant participant) {
        return new NoteResponseDto.ParticipantInfo(
                participant.getMember().getId(),
                participant.getMember().getName(),
                participant.getMember().getProfileImage(),
                participant.getIsOnline(),
                participant.getJoinedAt()
        );
    }

    /**
     * NoteParticipant 엔티티 생성
     */
    public static NoteParticipant toNoteParticipant(Note note, Member member) {
        return NoteParticipant.builder()
                .note(note)
                .member(member)
                .isOnline(true)
                .build();
    }

    /**
     * 노트 삭제 응답 DTO 생성
     */
    public static NoteResponseDto.Delete toDeleteResponse(Note note) {
        return new NoteResponseDto.Delete(
                "노트가 삭제되었습니다."
        );
    }

    /**
     * 노트 제목 수정 응답 DTO 생성
     */
    public static NoteResponseDto.UpdateTitleResponse toUpdateTitleResponse(Note note) {
        return new NoteResponseDto.UpdateTitleResponse(
                note.getId(),
                note.getTitle(),
                note.getLastModifiedAt()
        );
    }
}
