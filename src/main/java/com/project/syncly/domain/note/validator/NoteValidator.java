package com.project.syncly.domain.note.validator;

import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 노트 엔티티 유효성 검증 컴포넌트
 *
 * <p>노트의 접근 권한, 제목, 내용 등을 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NoteValidator {

    private final NoteRepository noteRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    // 제목 검증 규칙
    private static final int MIN_TITLE_LENGTH = 1;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final Pattern XSS_PATTERN = Pattern.compile("[<>\"'%;()&+]");

    // 내용 검증 규칙
    private static final long MAX_CONTENT_SIZE = 1024 * 1024; // 1MB

    /**
     * 노트 접근 권한 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>노트가 존재하는가?</li>
     *   <li>사용자가 워크스페이스 멤버인가?</li>
     *   <li>노트가 삭제되지 않았는가?</li>
     * </ul>
     *
     * @param noteId 노트 ID
     * @param workspaceId 워크스페이스 ID
     * @param memberId 멤버 ID
     * @return 유효한 노트 엔티티
     * @throws NoteException 접근 권한이 없거나 노트를 찾을 수 없음
     */
    public Note validateNoteAccess(Long noteId, Long workspaceId, Long memberId) {
        // 1. 워크스페이스 멤버 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, memberId)) {
            log.warn("워크스페이스 멤버 아님: workspaceId={}, memberId={}", workspaceId, memberId);
            throw new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER);
        }

        // 2. 노트 존재 확인
        Note note = noteRepository.findByIdAndWorkspaceId(noteId, workspaceId)
                .orElseThrow(() -> {
                    log.warn("노트를 찾을 수 없음: noteId={}, workspaceId={}", noteId, workspaceId);
                    return new NoteException(NoteErrorCode.NOTE_NOT_FOUND);
                });

        // 3. 삭제된 노트 확인
        if (note.getIsDeleted()) {
            log.warn("삭제된 노트에 접근 시도: noteId={}", noteId);
            throw new NoteException(NoteErrorCode.NOTE_ALREADY_DELETED);
        }

        return note;
    }

    /**
     * 노트 제목 유효성 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>제목이 비어있지 않은가?</li>
     *   <li>제목 길이가 1~200자인가?</li>
     *   <li>XSS 공격 특수문자가 없는가?</li>
     * </ul>
     *
     * @param title 검증할 제목
     * @throws NoteException 제목이 유효하지 않음
     */
    public void validateTitle(String title) {
        // null/empty 체크
        if (title == null || title.isBlank()) {
            log.warn("빈 제목으로 노트 생성 시도");
            throw new NoteException(NoteErrorCode.EMPTY_NOTE_TITLE);
        }

        String trimmedTitle = title.trim();

        // 길이 체크
        if (trimmedTitle.length() < MIN_TITLE_LENGTH) {
            log.warn("너무 짧은 제목: length={}", trimmedTitle.length());
            throw new NoteException(NoteErrorCode.EMPTY_NOTE_TITLE);
        }

        if (trimmedTitle.length() > MAX_TITLE_LENGTH) {
            log.warn("너무 긴 제목: length={}", trimmedTitle.length());
            throw new NoteException(NoteErrorCode.NOTE_TITLE_TOO_LONG);
        }

        // XSS 공격 특수문자 체크
        if (XSS_PATTERN.matcher(trimmedTitle).find()) {
            log.warn("제목에 XSS 위험 문자 포함: title={}", trimmedTitle);
            throw new NoteException(NoteErrorCode.INVALID_NOTE_TITLE, "제목에 특수문자가 포함되어 있습니다");
        }
    }

    /**
     * 노트 내용 유효성 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>내용 크기가 1MB 이하인가? (NULL 허용)</li>
     * </ul>
     *
     * @param content 검증할 내용 (null 허용)
     * @throws NoteException 내용이 너무 큼
     */
    public void validateContent(String content) {
        // null은 허용 (빈 노트 가능)
        if (content == null) {
            return;
        }

        // 크기 체크
        byte[] contentBytes = content.getBytes();
        if (contentBytes.length > MAX_CONTENT_SIZE) {
            log.warn("노트 내용이 너무 큼: size={}bytes (max={}bytes)",
                    contentBytes.length, MAX_CONTENT_SIZE);
            throw new NoteException(NoteErrorCode.NOTE_CONTENT_TOO_LONG);
        }
    }

    /**
     * 노트 생성 권한 검증 (작성자 확인)
     *
     * <p>노트를 삭제하거나 수정하려는 사용자가 작성자인지 확인합니다.
     *
     * @param note 검증할 노트
     * @param requesterId 요청 사용자 ID
     * @throws NoteException 요청 사용자가 작성자가 아님
     */
    public void validateNoteCreator(Note note, Long requesterId) {
        if (!note.getCreator().getId().equals(requesterId)) {
            log.warn("노트 작성자가 아닌 사용자가 수정 시도: noteId={}, requesterId={}, creatorId={}",
                    note.getId(), requesterId, note.getCreator().getId());
            throw new NoteException(NoteErrorCode.NOT_NOTE_CREATOR);
        }
    }

    /**
     * 노트 업데이트 데이터 검증
     *
     * <p>제목, 내용 모두 유효성 검증합니다.
     *
     * @param title 새 제목 (null이면 검증 생략)
     * @param content 새 내용 (null이면 검증 생략)
     */
    public void validateNoteUpdate(String title, String content) {
        if (title != null) {
            validateTitle(title);
        }

        if (content != null) {
            validateContent(content);
        }
    }

    /**
     * 노트 상태 검증
     *
     * <p>노트가 유효한 상태인지 확인합니다.
     *
     * @param note 검증할 노트
     * @throws NoteException 노트가 삭제된 상태
     */
    public void validateNoteNotDeleted(Note note) {
        if (note.getIsDeleted()) {
            log.warn("삭제된 노트 접근 시도: noteId={}", note.getId());
            throw new NoteException(NoteErrorCode.NOTE_ALREADY_DELETED);
        }
    }
}
