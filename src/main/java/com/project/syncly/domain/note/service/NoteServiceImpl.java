package com.project.syncly.domain.note.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.note.converter.NoteConverter;
import com.project.syncly.domain.note.dto.NoteRequestDto;
import com.project.syncly.domain.note.dto.NoteResponseDto;
import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.entity.NoteParticipant;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.domain.note.repository.NoteParticipantRepository;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import com.project.syncly.domain.workspace.repository.WorkspaceRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final NoteParticipantRepository noteParticipantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MemberRepository memberRepository;
    private final NoteRedisService noteRedisService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public NoteResponseDto.Create createNote(Long workspaceId, NoteRequestDto.Create requestDto, Long memberId) {
        log.info("Creating note in workspace {} by member {}", workspaceId, memberId);

        // 워크스페이스 멤버십 검증
        validateWorkspaceMembership(workspaceId, memberId);

        // 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // 멤버 조회
        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.MEMBER_NOT_FOUND));

        // 노트 생성
        Note note = NoteConverter.toNote(requestDto, workspace, creator);
        Note savedNote = noteRepository.save(note);

        log.info("Note created successfully: noteId={}", savedNote.getId());

        // WebSocket을 통해 모든 워크스페이스 멤버에게 새 노트 생성 알림
        broadcastNoteCreation(savedNote, workspace.getId());

        return NoteConverter.toCreateResponse(savedNote);
    }

    @Override
    public NoteResponseDto.NoteList getNoteList(Long workspaceId, Long memberId, Pageable pageable) {
        log.info("Getting note list for workspace {} by member {}", workspaceId, memberId);

        // 워크스페이스 멤버십 검증
        validateWorkspaceMembership(workspaceId, memberId);

        // 노트 목록 조회
        Page<Note> notePage = noteRepository.findByWorkspaceId(workspaceId, pageable);

        // 각 노트의 참여자 수 조회
        List<NoteResponseDto.ListItem> noteItems = notePage.getContent().stream()
                .map(note -> {
                    Long participantCount = noteParticipantRepository.countByNoteId(note.getId());
                    return NoteConverter.toListItemResponse(note, participantCount);
                })
                .toList();

        return new NoteResponseDto.NoteList(
                noteItems,
                notePage.getTotalElements(),
                notePage.getNumber(),
                notePage.getTotalPages()
        );
    }

    @Override
    public NoteResponseDto.Detail getNoteDetail(Long workspaceId, Long noteId, Long memberId) {
        log.info("Getting note detail: workspaceId={}, noteId={}, memberId={}", workspaceId, noteId, memberId);

        // 워크스페이스 멤버십 검증
        validateWorkspaceMembership(workspaceId, memberId);

        // 노트 조회 및 워크스페이스 일치 확인
        Note note = noteRepository.findByIdAndWorkspaceId(noteId, workspaceId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        // 활성 참여자 목록 조회
        List<NoteParticipant> activeParticipants = noteParticipantRepository.findOnlineParticipantsByNoteId(noteId);

        return NoteConverter.toDetailResponse(note, activeParticipants);
    }

    @Override
    @Transactional
    public NoteResponseDto.Delete deleteNote(Long workspaceId, Long noteId, Long memberId) {
        log.info("Deleting note: workspaceId={}, noteId={}, memberId={}", workspaceId, noteId, memberId);

        // 워크스페이스 멤버십 검증
        validateWorkspaceMembership(workspaceId, memberId);

        // 노트 조회 및 워크스페이스 일치 확인
        Note note = noteRepository.findByIdAndWorkspaceId(noteId, workspaceId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        // 소프트 삭제
        note.markAsDeleted();
        noteRepository.save(note);

        log.info("Note deleted successfully: noteId={}", noteId);

        // WebSocket을 통해 모든 워크스페이스 멤버에게 노트 삭제 알림
        broadcastNoteDeletion(noteId, workspaceId);

        return NoteConverter.toDeleteResponse(note);
    }

    @Override
    @Transactional
    public NoteResponseDto.UpdateTitleResponse updateNoteTitle(Long workspaceId, Long noteId, NoteRequestDto.UpdateTitle requestDto, Long memberId) {
        log.info("Updating note title: workspaceId={}, noteId={}, memberId={}", workspaceId, noteId, memberId);

        // 워크스페이스 멤버십 검증
        validateWorkspaceMembership(workspaceId, memberId);

        // 노트 조회 및 워크스페이스 일치 확인
        Note note = noteRepository.findByIdAndWorkspaceId(noteId, workspaceId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        // 제목 업데이트
        note.updateTitle(requestDto.title());
        noteRepository.save(note);

        log.info("Note title updated successfully: noteId={}, newTitle={}", noteId, requestDto.title());

        // WebSocket을 통해 모든 워크스페이스 멤버에게 노트 제목 변경 알림
        broadcastNoteTitleUpdate(note, workspaceId);

        return NoteConverter.toUpdateTitleResponse(note);
    }

    @Override
    public void validateWorkspaceMember(Long workspaceId, Long memberId) {
        validateWorkspaceMembership(workspaceId, memberId);
    }

    @Override
    public int getRevisionFromRedis(Long noteId) {
        return noteRedisService.getRevision(noteId);
    }

    /**
     * WebSocket을 통해 노트 삭제를 모든 워크스페이스 멤버에게 브로드캐스트합니다.
     */
    private void broadcastNoteDeletion(Long noteId, Long workspaceId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTE_DELETED");

            Map<String, Object> payload = new HashMap<>();
            payload.put("noteId", noteId);
            payload.put("workspaceId", workspaceId);

            message.put("payload", payload);

            String destination = "/topic/workspace/" + workspaceId + "/notes/list";
            messagingTemplate.convertAndSend(destination, message);

            log.info("Note deletion broadcasted: noteId={}, destination={}", noteId, destination);
        } catch (Exception e) {
            log.error("Failed to broadcast note deletion: noteId={}", noteId, e);
            // 브로드캐스트 실패는 note 삭제에 영향을 주지 않음
        }
    }

    /**
     * WebSocket을 통해 새로운 노트 생성을 모든 워크스페이스 멤버에게 브로드캐스트합니다.
     */
    private void broadcastNoteCreation(Note savedNote, Long workspaceId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTE_CREATED");

            Map<String, Object> payload = new HashMap<>();
            payload.put("noteId", savedNote.getId());
            payload.put("title", savedNote.getTitle());
            payload.put("creatorName", savedNote.getCreator().getName());
            payload.put("creatorProfileImage", savedNote.getCreator().getProfileImage());
            payload.put("createdAt", savedNote.getCreatedAt());
            payload.put("workspaceId", workspaceId);
            payload.put("participantCount", 0L);

            message.put("payload", payload);

            String destination = "/topic/workspace/" + workspaceId + "/notes/list";
            messagingTemplate.convertAndSend(destination, message);

            log.info("Note creation broadcasted: noteId={}, destination={}", savedNote.getId(), destination);
        } catch (Exception e) {
            log.error("Failed to broadcast note creation: noteId={}", savedNote.getId(), e);
            // 브로드캐스트 실패는 note 생성에 영향을 주지 않음
        }
    }

    /**
     * WebSocket을 통해 노트 제목 변경을 모든 워크스페이스 멤버에게 브로드캐스트합니다.
     */
    private void broadcastNoteTitleUpdate(Note note, Long workspaceId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NOTE_TITLE_UPDATED");

            Map<String, Object> payload = new HashMap<>();
            payload.put("noteId", note.getId());
            payload.put("title", note.getTitle());
            payload.put("workspaceId", workspaceId);
            payload.put("lastModifiedAt", note.getLastModifiedAt());

            message.put("payload", payload);

            String destination = "/topic/workspace/" + workspaceId + "/notes/list";
            messagingTemplate.convertAndSend(destination, message);

            log.info("Note title update broadcasted: noteId={}, newTitle={}, destination={}", note.getId(), note.getTitle(), destination);
        } catch (Exception e) {
            log.error("Failed to broadcast note title update: noteId={}", note.getId(), e);
            // 브로드캐스트 실패는 제목 수정에 영향을 주지 않음
        }
    }

    /**
     * 워크스페이스 멤버십 검증
     */
    private void validateWorkspaceMembership(Long workspaceId, Long memberId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, memberId)) {
            throw new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER);
        }
    }
}
