package com.project.syncly.domain.note.service;

import com.project.syncly.domain.note.dto.NoteRequestDto;
import com.project.syncly.domain.note.dto.NoteResponseDto;
import org.springframework.data.domain.Pageable;

public interface NoteService {

    /**
     * 노트 생성
     */
    NoteResponseDto.Create createNote(Long workspaceId, NoteRequestDto.Create requestDto, Long memberId);

    /**
     * 워크스페이스의 노트 목록 조회 (페이징)
     */
    NoteResponseDto.NoteList getNoteList(Long workspaceId, Long memberId, Pageable pageable);

    /**
     * 노트 상세 조회
     */
    NoteResponseDto.Detail getNoteDetail(Long workspaceId, Long noteId, Long memberId);

    /**
     * 노트 삭제 (소프트 삭제)
     */
    NoteResponseDto.Delete deleteNote(Long workspaceId, Long noteId, Long memberId);

    /**
     * 노트 제목 수정
     */
    NoteResponseDto.UpdateTitleResponse updateNoteTitle(Long workspaceId, Long noteId, NoteRequestDto.UpdateTitle requestDto, Long memberId);

    /**
     * 워크스페이스 멤버 권한 확인
     */
    void validateWorkspaceMember(Long workspaceId, Long memberId);
}
