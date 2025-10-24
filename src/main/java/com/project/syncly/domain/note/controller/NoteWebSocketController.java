package com.project.syncly.domain.note.controller;

import com.project.syncly.domain.note.dto.CursorPosition;
import com.project.syncly.domain.note.dto.EditOperation;
import com.project.syncly.domain.note.dto.NoteWebSocketDto;
import com.project.syncly.domain.note.dto.WebSocketMessage;
import com.project.syncly.domain.note.dto.WebSocketMessageType;
import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.entity.NoteParticipant;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.domain.note.repository.NoteParticipantRepository;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.note.service.NoteRedisService;
import com.project.syncly.domain.note.service.OTService;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.jwt.PrincipalDetails;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 실시간 협업 노트 WebSocket 메시지 핸들러
 *
 * <p>노트 입장/퇴장/편집 이벤트를 처리하고 참여자들에게 알림을 브로드캐스트합니다.
 *
 * <p><b>메시지 라우팅:</b>
 * <ul>
 *   <li>클라이언트 → /app/notes/{noteId}/enter - 노트 입장</li>
 *   <li>클라이언트 → /app/notes/{noteId}/leave - 노트 퇴장</li>
 *   <li>클라이언트 → /app/notes/{noteId}/edit - 편집 연산</li>
 *   <li>서버 → /topic/notes/{noteId}/participants - 참여자 변경 (브로드캐스트)</li>
 *   <li>서버 → /topic/notes/{noteId}/edits - 편집 연산 (브로드캐스트)</li>
 *   <li>서버 → /queue/notes/{noteId}/enter - 입장 응답 (유니캐스트)</li>
 *   <li>서버 → /queue/errors - 에러 메시지 (유니캐스트)</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NoteWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NoteRedisService noteRedisService;
    private final OTService otService;
    private final NoteRepository noteRepository;
    private final NoteParticipantRepository noteParticipantRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 전체 content를 브로드캐스트하는 주기 (연산 개수 기준)
     * 10개 연산마다 한 번씩 전체 content를 전송하여 클라이언트 동기화
     */
    private static final int FULL_CONTENT_BROADCAST_INTERVAL = 10;

    /**
     * 편집 재시도 최대 횟수
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 노트 입장 핸들러
     *
     * <p>사용자가 노트에 입장하면:
     * <ol>
     *   <li>NoteParticipant 엔티티 조회 또는 생성 (isOnline=true 설정)</li>
     *   <li>Redis에 사용자 추가 (NOTE:USERS:{noteId})</li>
     *   <li>WebSocket 세션 매핑 저장 (WS:NOTE_SESSIONS:{sessionId} → noteId:workspaceMemberId)</li>
     *   <li>Redis에서 노트 내용, 버전, 활성 사용자 조회</li>
     *   <li>입장한 사용자에게 EnterResponse 전송 (유니캐스트)</li>
     *   <li>다른 참여자들에게 UserJoinedMessage 브로드캐스트</li>
     * </ol>
     *
     * @param noteId 노트 ID
     * @param principal 인증된 사용자 정보 (memberId)
     * @param headerAccessor WebSocket 세션 정보
     */
    @MessageMapping("/notes/{noteId}/enter")
    public void handleEnter(
            @DestinationVariable Long noteId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long memberId = extractMemberId(principal);
        String sessionId = headerAccessor.getSessionId();

        log.info("노트 입장 요청: noteId={}, memberId={}, sessionId={}", noteId, memberId, sessionId);

        // 1. 노트 존재 확인
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        Long workspaceId = note.getWorkspace().getId();

        // 2. 워크스페이스 멤버 확인
        WorkspaceMember workspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

        Long workspaceMemberId = workspaceMember.getId();
        String userName = workspaceMember.getName();
        String profileImage = workspaceMember.getProfileImage();

        // 3. NoteParticipant 조회 또는 생성 (재시도 로직 포함)
        NoteParticipant participant = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                participant = getOrCreateParticipantInTransaction(noteId, memberId, note, workspaceMember);
                break;
            } catch (Exception e) {
                if (attempt == 2) {
                    log.error("참여자 생성 최종 실패: noteId={}, memberId={}", noteId, memberId, e);
                    throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
                }
                log.warn("참여자 생성 실패 ({}회 재시도): noteId={}, memberId={}, error={}",
                        attempt + 1, noteId, memberId, e.getMessage());
                try {
                    Thread.sleep(100L * (attempt + 1)); // exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
                }
            }
        }

        // 5. Redis에 사용자 추가
        noteRedisService.addUser(noteId, workspaceMemberId);

        // 6. WebSocket 세션 매핑 저장 (disconnect 시 자동 정리용)
        String sessionData = noteId + ":" + workspaceMemberId;
        redisTemplate.opsForHash().put(
                RedisKeyPrefix.WS_NOTE_SESSIONS.get(),
                sessionId,
                sessionData
        );

        // 7. Redis에서 노트 데이터 조회 (없으면 DB에서 초기화)
        String content = noteRedisService.getContent(noteId);
        if (content == null) {
            // Redis에 없으면 DB에서 로드하여 초기화
            noteRedisService.initializeNote(noteId, note.getContent());
            content = note.getContent();
        }

        Integer revision = noteRedisService.getRevision(noteId);
        List<Long> activeUserIds = noteRedisService.getActiveUsers(noteId).stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 8. 입장한 사용자에게 EnterResponse 전송 (유니캐스트)
        NoteWebSocketDto.EnterResponse enterResponse = new NoteWebSocketDto.EnterResponse(
                noteId,
                note.getTitle(),
                content,
                revision,
                activeUserIds,
                LocalDateTime.now()
        );

        WebSocketMessage<NoteWebSocketDto.EnterResponse> enterMessage = WebSocketMessage.of(
                WebSocketMessageType.ENTER,
                enterResponse,
                workspaceMemberId
        );

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/notes/" + noteId + "/enter",
                enterMessage
        );

        // 9. 다른 참여자들에게 UserJoinedMessage 브로드캐스트
        NoteWebSocketDto.UserJoinedMessage joinedMessage = new NoteWebSocketDto.UserJoinedMessage(
                workspaceMemberId,
                userName,
                profileImage,
                activeUserIds.size(),
                LocalDateTime.now()
        );

        WebSocketMessage<NoteWebSocketDto.UserJoinedMessage> broadcastMessage = WebSocketMessage.of(
                WebSocketMessageType.ENTER,
                joinedMessage,
                workspaceMemberId
        );

        messagingTemplate.convertAndSend(
                "/topic/notes/" + noteId + "/participants",
                broadcastMessage
        );

        log.info("노트 입장 완료: noteId={}, workspaceMemberId={}, activeUsers={}",
                noteId, workspaceMemberId, activeUserIds.size());
    }

    /**
     * 노트 퇴장 핸들러
     *
     * <p>사용자가 노트에서 퇴장하면:
     * <ol>
     *   <li>NoteParticipant의 isOnline 상태를 false로 변경</li>
     *   <li>Redis에서 사용자 제거 (NOTE:USERS:{noteId})</li>
     *   <li>WebSocket 세션 매핑 삭제</li>
     *   <li>퇴장한 사용자에게 LeaveResponse 전송 (유니캐스트)</li>
     *   <li>다른 참여자들에게 UserLeftMessage 브로드캐스트</li>
     * </ol>
     *
     * @param noteId 노트 ID
     * @param principal 인증된 사용자 정보 (memberId)
     * @param headerAccessor WebSocket 세션 정보
     */
    @MessageMapping("/notes/{noteId}/leave")
    @Transactional
    public void handleLeave(
            @DestinationVariable Long noteId,
            Principal principal,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Long memberId = extractMemberId(principal);
        String sessionId = headerAccessor.getSessionId();

        log.info("노트 퇴장 요청: noteId={}, memberId={}, sessionId={}", noteId, memberId, sessionId);

        // 1. 노트 존재 확인
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        Long workspaceId = note.getWorkspace().getId();

        // 2. 워크스페이스 멤버 확인
        WorkspaceMember workspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

        Long workspaceMemberId = workspaceMember.getId();
        String userName = workspaceMember.getName();

        // 3. NoteParticipant 오프라인 상태로 변경
        noteParticipantRepository.updateToOffline(noteId, memberId);

        // 4. Redis에서 사용자 제거
        noteRedisService.removeUser(noteId, workspaceMemberId);

        // 5. WebSocket 세션 매핑 삭제
        redisTemplate.opsForHash().delete(
                RedisKeyPrefix.WS_NOTE_SESSIONS.get(),
                sessionId
        );

        // 6. 현재 활성 사용자 수 조회
        int activeUserCount = noteRedisService.getActiveUsers(noteId).size();

        // 7. 퇴장한 사용자에게 LeaveResponse 전송 (유니캐스트)
        NoteWebSocketDto.LeaveResponse leaveResponse = new NoteWebSocketDto.LeaveResponse(
                noteId,
                "노트에서 퇴장했습니다.",
                LocalDateTime.now()
        );

        WebSocketMessage<NoteWebSocketDto.LeaveResponse> leaveMessage = WebSocketMessage.of(
                WebSocketMessageType.LEAVE,
                leaveResponse,
                workspaceMemberId
        );

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/notes/" + noteId + "/leave",
                leaveMessage
        );

        // 8. 다른 참여자들에게 UserLeftMessage 브로드캐스트
        NoteWebSocketDto.UserLeftMessage leftMessage = new NoteWebSocketDto.UserLeftMessage(
                workspaceMemberId,
                userName,
                activeUserCount,
                LocalDateTime.now()
        );

        WebSocketMessage<NoteWebSocketDto.UserLeftMessage> broadcastMessage = WebSocketMessage.of(
                WebSocketMessageType.LEAVE,
                leftMessage,
                workspaceMemberId
        );

        messagingTemplate.convertAndSend(
                "/topic/notes/" + noteId + "/participants",
                broadcastMessage
        );

        log.info("노트 퇴장 완료: noteId={}, workspaceMemberId={}, remainingUsers={}",
                noteId, workspaceMemberId, activeUserCount);
    }

    /**
     * 노트 편집 핸들러
     *
     * <p>사용자의 편집 연산을 처리하고 OT 알고리즘을 적용하여 다른 참여자들에게 브로드캐스트합니다.
     *
     * <p><b>처리 흐름:</b>
     * <ol>
     *   <li>사용자 권한 검증 (해당 노트의 active user인지)</li>
     *   <li>OTService.processEdit() 호출 (transform + 적용 + Redis 저장)</li>
     *   <li>성공 시 모든 참여자에게 EditBroadcastMessage 전송</li>
     *   <li>10개 연산마다 전체 content 포함 (동기화)</li>
     *   <li>실패 시 재시도 (최대 3회)</li>
     * </ol>
     *
     * @param noteId 노트 ID
     * @param request 편집 요청 (EditOperation 포함)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/{noteId}/edit")
    public void handleEdit(
            @DestinationVariable Long noteId,
            @Payload NoteWebSocketDto.EditRequest request,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);
        EditOperation operation = request.operation();

        log.debug("편집 요청 수신: noteId={}, memberId={}, operation={}",
                noteId, memberId, operation);

        try {
            // 1. 노트 존재 확인
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

            Long workspaceId = note.getWorkspace().getId();

            // 2. 워크스페이스 멤버 확인
            WorkspaceMember workspaceMember = workspaceMemberRepository
                    .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

            Long workspaceMemberId = workspaceMember.getId();
            String userName = workspaceMember.getName();

            // 3. 활성 사용자 검증 (노트에 입장한 상태인지)
            if (!noteRedisService.getActiveUsers(noteId).contains(String.valueOf(workspaceMemberId))) {
                log.warn("비활성 사용자의 편집 시도: noteId={}, workspaceMemberId={}", noteId, workspaceMemberId);
                sendErrorToUser(principal.getName(), noteId,
                        NoteErrorCode.NOTE_ACCESS_DENIED.getCode(),
                        "노트에 먼저 입장해주세요.");
                return;
            }

            // 4. 편집 처리 (재시도 로직 포함)
            OTService.ProcessEditResult result = processEditWithRetry(noteId, operation, MAX_RETRY_COUNT);

            log.info("편집 성공: noteId={}, workspaceMemberId={}, type={}, position={}, " +
                            "originalRevision={}, newRevision={}, contentLength={}",
                    noteId, workspaceMemberId, operation.getType(), operation.getPosition(),
                    operation.getRevision(), result.revision(), result.content().length());

            // 5. 전체 content 포함 여부 결정 (10개 연산마다)
            boolean includeFullContent = (result.revision() % FULL_CONTENT_BROADCAST_INTERVAL == 0);
            String contentToSend = includeFullContent ? result.content() : null;

            // 6. 모든 참여자에게 EditBroadcastMessage 브로드캐스트
            NoteWebSocketDto.EditBroadcastMessage broadcastMessage =
                    new NoteWebSocketDto.EditBroadcastMessage(
                            result.appliedOperation(),
                            contentToSend,
                            result.revision(),
                            workspaceMemberId,
                            userName,
                            LocalDateTime.now(),
                            includeFullContent
                    );

            WebSocketMessage<NoteWebSocketDto.EditBroadcastMessage> message = WebSocketMessage.of(
                    WebSocketMessageType.EDIT,
                    broadcastMessage,
                    workspaceMemberId
            );

            messagingTemplate.convertAndSend(
                    "/topic/notes/" + noteId + "/edits",
                    message
            );

            log.debug("편집 브로드캐스트 완료: noteId={}, revision={}, includeFullContent={}",
                    noteId, result.revision(), includeFullContent);

        } catch (NoteException e) {
            log.error("편집 처리 중 NoteException 발생: noteId={}, memberId={}, error={}",
                    noteId, memberId, e.getMessage());
            handleEditError(principal.getName(), noteId, e);

        } catch (Exception e) {
            log.error("편집 처리 중 예상치 못한 에러 발생: noteId={}, memberId={}, error={}",
                    noteId, memberId, e.getMessage(), e);
            sendErrorToUser(principal.getName(), noteId,
                    NoteErrorCode.OT_TRANSFORM_FAILED.getCode(),
                    "편집 처리 중 오류가 발생했습니다. 페이지를 새로고침해주세요.");
        }
    }

    /**
     * 편집 처리 with 재시도 로직
     *
     * <p>동시 편집으로 인한 충돌 발생 시 최대 MAX_RETRY_COUNT번까지 재시도합니다.
     *
     * @param noteId 노트 ID
     * @param operation 편집 연산
     * @param maxRetries 최대 재시도 횟수
     * @return 편집 처리 결과
     * @throws NoteException 재시도 후에도 실패한 경우
     */
    private OTService.ProcessEditResult processEditWithRetry(
            Long noteId,
            EditOperation operation,
            int maxRetries
    ) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return otService.processEdit(noteId, operation);

            } catch (NoteException e) {
                // INVALID_OPERATION은 재시도해도 소용없음 → 즉시 throw
                if (e.getCode() == NoteErrorCode.INVALID_OPERATION) {
                    throw e;
                }

                lastException = e;
                attempt++;

                if (attempt < maxRetries) {
                    log.warn("편집 처리 실패, 재시도 {}/{}: noteId={}, error={}",
                            attempt, maxRetries, noteId, e.getMessage());

                    // 짧은 대기 후 재시도 (exponential backoff)
                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new NoteException(NoteErrorCode.OT_TRANSFORM_FAILED);
                    }
                } else {
                    log.error("편집 처리 최종 실패: noteId={}, attempts={}, error={}",
                            noteId, attempt, e.getMessage());
                }
            }
        }

        // 모든 재시도 실패
        throw new NoteException(NoteErrorCode.CONCURRENT_EDIT_CONFLICT,
                "동시 편집 충돌이 계속 발생합니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 편집 에러 처리
     *
     * <p>에러 유형에 따라 적절한 에러 메시지와 동기화 정보를 클라이언트에 전송합니다.
     *
     * @param userId 사용자 ID (memberId)
     * @param noteId 노트 ID
     * @param exception 발생한 예외
     */
    private void handleEditError(String userId, Long noteId, NoteException exception) {
        com.project.syncly.global.apiPayload.code.BaseErrorCode baseErrorCode = exception.getCode();
        String errorCodeStr = baseErrorCode.getCode();

        // 동기화가 필요한 에러인 경우 현재 content와 revision 전송
        if (errorCodeStr.equals(NoteErrorCode.INVALID_OPERATION.getCode()) ||
            errorCodeStr.equals(NoteErrorCode.REVISION_MISMATCH.getCode()) ||
            errorCodeStr.equals(NoteErrorCode.CONCURRENT_EDIT_CONFLICT.getCode())) {

            try {
                String currentContent = noteRedisService.getContent(noteId);
                int currentRevision = noteRedisService.getRevision(noteId);

                NoteWebSocketDto.ErrorMessage errorMessage = NoteWebSocketDto.ErrorMessage.withSync(
                        errorCodeStr,
                        exception.getMessage(),
                        currentContent,
                        currentRevision
                );

                WebSocketMessage<NoteWebSocketDto.ErrorMessage> message = WebSocketMessage.of(
                        WebSocketMessageType.ERROR,
                        errorMessage,
                        null
                );

                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/errors",
                        message
                );

                log.info("동기화 에러 메시지 전송: userId={}, noteId={}, errorCode={}",
                        userId, noteId, errorCodeStr);

            } catch (Exception e) {
                log.error("에러 처리 중 또 다른 에러 발생: userId={}, noteId={}, error={}",
                        userId, noteId, e.getMessage());
                // 최소한의 에러 메시지라도 전송
                sendErrorToUser(userId, noteId, errorCodeStr, exception.getMessage());
            }
        } else {
            // 단순 에러 메시지만 전송
            sendErrorToUser(userId, noteId, errorCodeStr, exception.getMessage());
        }
    }

    /**
     * 사용자에게 에러 메시지 전송
     *
     * @param userId 사용자 ID (memberId)
     * @param noteId 노트 ID
     * @param errorCode 에러 코드
     * @param errorMessage 에러 메시지
     */
    private void sendErrorToUser(String userId, Long noteId, String errorCode, String errorMessage) {
        NoteWebSocketDto.ErrorMessage error = NoteWebSocketDto.ErrorMessage.of(errorCode, errorMessage);

        WebSocketMessage<NoteWebSocketDto.ErrorMessage> message = WebSocketMessage.of(
                WebSocketMessageType.ERROR,
                error,
                null
        );

        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/errors",
                message
        );

        log.debug("에러 메시지 전송: userId={}, noteId={}, errorCode={}", userId, noteId, errorCode);
    }

    /**
     * 노트 생성 핸들러
     *
     * <p>사용자가 새로운 노트를 생성합니다.
     *
     * @param request 노트 생성 요청 (title)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/create")
    @Transactional
    public void handleCreate(
            @Payload NoteWebSocketDto.CreateRequest request,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);

        log.info("노트 생성 요청: memberId={}, title={}", memberId, request.title());

        try {
            // 1. workspaceId 검증
            Long workspaceId = request.workspaceId();
            if (workspaceId == null || workspaceId <= 0) {
                throw new NoteException(NoteErrorCode.NOTE_NOT_FOUND);
            }

            // 2. 워크스페이스 멤버 확인
            WorkspaceMember workspaceMember = workspaceMemberRepository
                    .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

            // 3. 노트 생성
            Note newNote = Note.builder()
                    .title(request.title())
                    .content("")
                    .workspace(workspaceMember.getWorkspace())
                    .creator(workspaceMember.getMember())
                    .build();

            Note createdNote = noteRepository.save(newNote);

            // 4. Redis 초기화
            noteRedisService.initializeNote(createdNote.getId(), "");

            // 5. 현재 사용자를 자동으로 참여자에 추가
            NoteParticipant participant = NoteParticipant.builder()
                    .note(createdNote)
                    .member(workspaceMember.getMember())
                    .isOnline(false)
                    .build();
            noteParticipantRepository.save(participant);

            // 6. 생성 응답 전송 (요청한 사용자에게만)
            NoteWebSocketDto.CreateResponse response = new NoteWebSocketDto.CreateResponse(
                    createdNote.getId(),
                    createdNote.getTitle(),
                    workspaceId,
                    workspaceMember.getName(),
                    workspaceMember.getProfileImage(),
                    createdNote.getCreatedAt()
            );

            WebSocketMessage<NoteWebSocketDto.CreateResponse> message = WebSocketMessage.of(
                    WebSocketMessageType.CREATE,
                    response,
                    null
            );

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/notes/create",
                    message
            );

            // 7. 같은 워크스페이스를 보고 있는 모든 사용자에게 브로드캐스트
            NoteWebSocketDto.NoteCreatedMessage broadcastMessage = new NoteWebSocketDto.NoteCreatedMessage(
                    createdNote.getId(),
                    createdNote.getTitle(),
                    workspaceId,
                    workspaceMember.getName(),
                    workspaceMember.getProfileImage(),
                    createdNote.getCreatedAt()
            );

            WebSocketMessage<NoteWebSocketDto.NoteCreatedMessage> broadcastMsg = WebSocketMessage.of(
                    WebSocketMessageType.NOTE_CREATED,
                    broadcastMessage,
                    null
            );

            messagingTemplate.convertAndSend(
                    "/topic/workspace/" + workspaceId + "/notes/list",
                    broadcastMsg
            );

            log.info("노트 생성 완료: noteId={}, title={}, 브로드캐스트 완료", createdNote.getId(), createdNote.getTitle());

        } catch (NoteException e) {
            log.error("노트 생성 중 에러 발생: memberId={}, error={}", memberId, e.getMessage());
            sendErrorToUser(principal.getName(), null,
                    e.getCode().getCode(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("노트 생성 중 예상치 못한 에러 발생: memberId={}, error={}", memberId, e.getMessage(), e);
            sendErrorToUser(principal.getName(), null,
                    "INTERNAL_ERROR",
                    "노트 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 노트 목록 조회 핸들러
     *
     * <p>사용자가 노트 목록을 조회합니다.
     *
     * @param request 노트 목록 조회 요청 (page, size, sortBy, direction)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/list")
    public void handleList(
            @Payload NoteWebSocketDto.ListRequest request,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);

        log.info("노트 목록 조회 요청: memberId={}, page={}, size={}", memberId, request.page(), request.size());

        try {
            // 1. workspaceId 검증
            Long workspaceId = request.workspaceId();
            if (workspaceId == null || workspaceId <= 0) {
                throw new NoteException(NoteErrorCode.NOTE_NOT_FOUND);
            }

            // 2. 워크스페이스 멤버 확인
            WorkspaceMember workspaceMember = workspaceMemberRepository
                    .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

            // 3. 노트 목록 조회 (HTTP 메서드의 로직과 동일하게 처리)
            // org.springframework.data.domain.PageRequest를 사용해서 페이징 처리
            var pageRequest = org.springframework.data.domain.PageRequest.of(
                    request.page(),
                    request.size(),
                    org.springframework.data.domain.Sort.Direction.fromString(request.direction()),
                    request.sortBy()
            );

            var notesPage = noteRepository.findByWorkspaceId(workspaceId, pageRequest);

            // 4. NoteListItem으로 변환
            List<NoteWebSocketDto.NoteListItem> noteItems = notesPage.getContent().stream()
                    .map(note -> new NoteWebSocketDto.NoteListItem(
                            note.getId(),
                            note.getTitle(),
                            note.getCreator().getName(),
                            note.getCreator().getProfileImage(),
                            note.getLastModifiedAt(),
                            Math.toIntExact(noteParticipantRepository.countByNoteId(note.getId())),
                            note.getCreatedAt()
                    ))
                    .toList();

            // 4. 응답 생성
            NoteWebSocketDto.ListResponse response = new NoteWebSocketDto.ListResponse(
                    noteItems,
                    (int) notesPage.getTotalElements(),
                    notesPage.getNumber(),
                    notesPage.getTotalPages()
            );

            WebSocketMessage<NoteWebSocketDto.ListResponse> message = WebSocketMessage.of(
                    WebSocketMessageType.LIST,
                    response,
                    null
            );

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/notes/list",
                    message
            );

            log.info("노트 목록 조회 완료: memberId={}, count={}", memberId, noteItems.size());

        } catch (NoteException e) {
            log.error("노트 목록 조회 중 에러 발생: memberId={}, error={}", memberId, e.getMessage());
            sendErrorToUser(principal.getName(), null,
                    e.getCode().getCode(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("노트 목록 조회 중 예상치 못한 에러 발생: memberId={}, error={}", memberId, e.getMessage(), e);
            sendErrorToUser(principal.getName(), null,
                    "INTERNAL_ERROR",
                    "노트 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 노트 상세 조회 핸들러
     *
     * <p>사용자가 특정 노트의 상세 정보를 조회합니다.
     *
     * @param request 노트 상세 조회 요청 (noteId)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/detail")
    public void handleGetDetail(
            @Payload NoteWebSocketDto.GetDetailRequest request,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);
        Long noteId = request.noteId();

        log.info("노트 상세 조회 요청: memberId={}, noteId={}", memberId, noteId);

        try {
            // 1. 노트 존재 확인
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

            Long workspaceId = note.getWorkspace().getId();

            // 2. 워크스페이스 멤버 확인
            WorkspaceMember workspaceMember = workspaceMemberRepository
                    .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

            // 3. Redis에서 내용 및 버전 조회
            String content = noteRedisService.getContent(noteId);
            if (content == null) {
                content = note.getContent();
                noteRedisService.initializeNote(noteId, content);
            }

            Integer revision = noteRedisService.getRevision(noteId);

            // 4. 활성 참여자 정보 조회
            List<NoteWebSocketDto.ActiveUserInfo> activeParticipants = noteRedisService.getActiveUsers(noteId)
                    .stream()
                    .map(userId -> {
                        Long workspaceMemberId = Long.valueOf(userId);
                        // 워크스페이스 멤버 정보 조회
                        try {
                            WorkspaceMember wm = workspaceMemberRepository.findById(workspaceMemberId).orElse(null);
                            if (wm != null) {
                                return new NoteWebSocketDto.ActiveUserInfo(
                                        workspaceMemberId,
                                        wm.getName(),
                                        wm.getProfileImage(),
                                        com.project.syncly.domain.note.util.UserColorGenerator.generateColor(workspaceMemberId)
                                );
                            }
                        } catch (Exception e) {
                            log.warn("워크스페이스 멤버 조회 실패: workspaceMemberId={}", workspaceMemberId, e);
                        }
                        // Fallback: 정보 없을 경우
                        return new NoteWebSocketDto.ActiveUserInfo(
                                workspaceMemberId,
                                "Unknown User",
                                null,
                                com.project.syncly.domain.note.util.UserColorGenerator.generateColor(workspaceMemberId)
                        );
                    })
                    .collect(java.util.stream.Collectors.toList());

            // 5. 응답 생성
            NoteWebSocketDto.GetDetailResponse response = new NoteWebSocketDto.GetDetailResponse(
                    note.getId(),
                    note.getTitle(),
                    content,
                    workspaceId,
                    note.getCreator().getId(),
                    note.getCreator().getName(),
                    note.getCreator().getProfileImage(),
                    activeParticipants,
                    note.getLastModifiedAt(),
                    note.getCreatedAt(),
                    revision
            );

            WebSocketMessage<NoteWebSocketDto.GetDetailResponse> message = WebSocketMessage.of(
                    WebSocketMessageType.GET_DETAIL,
                    response,
                    null
            );

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/notes/" + noteId + "/detail",
                    message
            );

            log.info("노트 상세 조회 완료: noteId={}, contentLength={}", noteId, content.length());

        } catch (NoteException e) {
            log.error("노트 상세 조회 중 에러 발생: memberId={}, noteId={}, error={}", memberId, noteId, e.getMessage());
            sendErrorToUser(principal.getName(), noteId,
                    e.getCode().getCode(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("노트 상세 조회 중 예상치 못한 에러 발생: memberId={}, noteId={}, error={}", memberId, noteId, e.getMessage(), e);
            sendErrorToUser(principal.getName(), noteId,
                    "INTERNAL_ERROR",
                    "노트 상세 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 노트 삭제 핸들러
     *
     * <p>사용자가 노트를 삭제합니다.
     *
     * @param request 노트 삭제 요청 (noteId)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/delete")
    @Transactional
    public void handleDelete(
            @Payload NoteWebSocketDto.DeleteRequest request,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);
        Long noteId = request.noteId();

        log.info("노트 삭제 요청: memberId={}, noteId={}", memberId, noteId);

        try {
            // 1. 노트 존재 확인
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

            // 2. 노트 소유자 확인 (소유자만 삭제 가능)
            if (!note.getCreator().getId().equals(memberId)) {
                throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
            }

            // 3. 노트 삭제 (소프트 삭제)
            note.markAsDeleted();
            noteRepository.save(note);

            // 4. Redis 정리 (편집 데이터 및 사용자 정보 삭제)
            noteRedisService.deleteNoteData(noteId);

            Long workspaceId = note.getWorkspace().getId();

            // 5. 응답 전송 (요청한 사용자에게만)
            NoteWebSocketDto.DeleteResponse response = new NoteWebSocketDto.DeleteResponse(
                    true,
                    "노트가 삭제되었습니다.",
                    LocalDateTime.now()
            );

            WebSocketMessage<NoteWebSocketDto.DeleteResponse> message = WebSocketMessage.of(
                    WebSocketMessageType.DELETE,
                    response,
                    null
            );

            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/notes/" + noteId + "/delete",
                    message
            );

            // 6. 같은 워크스페이스를 보고 있는 모든 사용자에게 브로드캐스트
            NoteWebSocketDto.NoteDeletedMessage broadcastMessage = new NoteWebSocketDto.NoteDeletedMessage(
                    noteId,
                    workspaceId,
                    LocalDateTime.now()
            );

            WebSocketMessage<NoteWebSocketDto.NoteDeletedMessage> broadcastMsg = WebSocketMessage.of(
                    WebSocketMessageType.NOTE_DELETED,
                    broadcastMessage,
                    null
            );

            messagingTemplate.convertAndSend(
                    "/topic/workspace/" + workspaceId + "/notes/list",
                    broadcastMsg
            );

            log.info("노트 삭제 완료: noteId={}, 브로드캐스트 완료", noteId);

        } catch (NoteException e) {
            log.error("노트 삭제 중 에러 발생: memberId={}, noteId={}, error={}", memberId, noteId, e.getMessage());
            sendErrorToUser(principal.getName(), noteId,
                    e.getCode().getCode(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("노트 삭제 중 예상치 못한 에러 발생: memberId={}, noteId={}, error={}", memberId, noteId, e.getMessage(), e);
            sendErrorToUser(principal.getName(), noteId,
                    "INTERNAL_ERROR",
                    "노트 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 노트 수동 저장 핸들러
     *
     * <p>사용자가 수동으로 저장 버튼을 클릭했을 때 호출됩니다.
     * Redis의 현재 내용을 DB에 저장합니다.
     *
     * @param noteId 노트 ID (URL 경로에서 추출)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/{noteId}/save")
    @Transactional
    public void handleManualSave(
            @DestinationVariable Long noteId,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);

        log.info("노트 수동 저장 요청: memberId={}, noteId={}", memberId, noteId);

        try {
            // 1. 노트 존재 확인
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

            Long workspaceId = note.getWorkspace().getId();

            // 2. 워크스페이스 멤버 확인
            WorkspaceMember workspaceMember = workspaceMemberRepository
                    .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

            // 3. Redis에서 현재 content와 revision 조회
            String currentContent = noteRedisService.getContent(noteId);
            int currentRevision = noteRedisService.getRevision(noteId);

            // 4. DB에 저장 (updateContent 메서드 사용)
            note.updateContent(currentContent);
            noteRepository.save(note);

            // 5. 모든 참여자에게 저장 완료 메시지 브로드캐스트
            NoteWebSocketDto.SaveResponse response = new NoteWebSocketDto.SaveResponse(
                    noteId,
                    currentRevision,
                    LocalDateTime.now(),
                    "수동 저장되었습니다.",
                    workspaceMember.getId(),
                    workspaceMember.getName()
            );

            WebSocketMessage<NoteWebSocketDto.SaveResponse> message = WebSocketMessage.of(
                    WebSocketMessageType.MANUAL_SAVE,
                    response,
                    null
            );

            messagingTemplate.convertAndSend(
                    "/topic/notes/" + noteId + "/save",
                    message
            );

            log.info("노트 수동 저장 완료: noteId={}, revision={}", noteId, currentRevision);

        } catch (NoteException e) {
            log.error("노트 저장 중 에러 발생: memberId={}, noteId={}, error={}", memberId, noteId, e.getMessage());
            sendErrorToUser(principal.getName(), noteId,
                    e.getCode().getCode(),
                    e.getMessage());
        } catch (Exception e) {
            log.error("노트 저장 중 예상치 못한 에러 발생: memberId={}, noteId={}, error={}", memberId, noteId, e.getMessage(), e);
            sendErrorToUser(principal.getName(), noteId,
                    "INTERNAL_ERROR",
                    "노트 저장 중 오류가 발생했습니다.");
        }
    }

    /**
     * 커서 위치 업데이트 핸들러
     *
     * <p>사용자의 커서 위치를 Redis에 저장하고 다른 참여자들에게 브로드캐스트합니다.
     *
     * <p><b>처리 흐름:</b>
     * <ol>
     *   <li>사용자 권한 검증 (활성 사용자인지)</li>
     *   <li>CursorPosition 객체 생성 (위치, 범위, 사용자 정보, 색상)</li>
     *   <li>Redis에 커서 저장 (TTL 10분)</li>
     *   <li>본인을 제외한 다른 참여자들에게 브로드캐스트</li>
     * </ol>
     *
     * <p><b>성능 최적화:</b>
     * <ul>
     *   <li>클라이언트는 100ms throttling 권장</li>
     *   <li>Redis TTL 10분 (자동 정리)</li>
     * </ul>
     *
     * @param noteId 노트 ID
     * @param request 커서 업데이트 요청 (position, range)
     * @param principal 인증된 사용자 정보 (memberId)
     */
    @MessageMapping("/notes/{noteId}/cursor")
    public void handleCursorUpdate(
            @DestinationVariable Long noteId,
            @Payload NoteWebSocketDto.CursorUpdateRequest request,
            Principal principal
    ) {
        Long memberId = extractMemberId(principal);

        try {
            // 1. 노트 존재 확인
            Note note = noteRepository.findById(noteId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

            Long workspaceId = note.getWorkspace().getId();

            // 2. 워크스페이스 멤버 확인
            WorkspaceMember workspaceMember = workspaceMemberRepository
                    .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                    .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

            Long workspaceMemberId = workspaceMember.getId();

            // 3. 활성 사용자 검증
            if (!noteRedisService.getActiveUsers(noteId).contains(String.valueOf(workspaceMemberId))) {
                log.warn("비활성 사용자의 커서 업데이트 시도: noteId={}, workspaceMemberId={}", noteId, workspaceMemberId);
                return;
            }

            // 4. CursorPosition 객체 생성
            CursorPosition cursor = CursorPosition.builder()
                    .position(request.position())
                    .range(request.range())
                    .workspaceMemberId(workspaceMemberId)
                    .userName(workspaceMember.getName())
                    .profileImage(workspaceMember.getProfileImage())
                    .color(com.project.syncly.domain.note.util.UserColorGenerator.generateColor(workspaceMemberId))
                    .build();

            // 5. Redis에 저장
            noteRedisService.setCursor(noteId, workspaceMemberId, cursor);

            // 6. 다른 참여자들에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/notes/" + noteId + "/cursors",
                    cursor
            );

            log.debug("커서 업데이트 브로드캐스트: noteId={}, workspaceMemberId={}, position={}",
                    noteId, workspaceMemberId, request.position());

        } catch (NoteException e) {
            log.error("커서 업데이트 중 에러 발생: noteId={}, memberId={}, error={}",
                    noteId, memberId, e.getMessage());
        } catch (Exception e) {
            log.error("커서 업데이트 중 예상치 못한 에러 발생: noteId={}, memberId={}, error={}",
                    noteId, memberId, e.getMessage(), e);
        }
    }

    /**
     * 참여자 조회 또는 생성 (동시성 안전 처리)
     *
     * <p>각 호출이 새로운 트랜잭션에서 실행되므로,
     * 이전 트랜잭션의 exception이 영향을 주지 않습니다.
     *
     * @param noteId 노트 ID
     * @param memberId 멤버 ID
     * @param note 노트 엔티티
     * @param workspaceMember 워크스페이스 멤버 엔티티
     * @return 참여자 엔티티 (생성되거나 기존 것)
     * @throws RuntimeException 재시도 필요한 경우
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private NoteParticipant getOrCreateParticipantInTransaction(
            Long noteId,
            Long memberId,
            Note note,
            WorkspaceMember workspaceMember
    ) {
        // 1. 먼저 기존 참여자 조회
        Optional<NoteParticipant> existing = noteParticipantRepository
                .findByNoteIdAndMemberId(noteId, memberId);

        if (existing.isPresent()) {
            NoteParticipant participant = existing.get();
            // 온라인 상태 업데이트
            if (!participant.getIsOnline()) {
                participant.setOnline();
                noteParticipantRepository.save(participant);
            }
            log.debug("기존 참여자 업데이트: noteId={}, memberId={}", noteId, memberId);
            return participant;
        }

        // 2. 새로운 참여자 생성
        NoteParticipant newParticipant = NoteParticipant.builder()
                .note(note)
                .member(workspaceMember.getMember())
                .isOnline(true)
                .build();

        try {
            NoteParticipant saved = noteParticipantRepository.save(newParticipant);
            log.debug("새 참여자 생성: noteId={}, memberId={}", noteId, memberId);
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시 요청으로 인한 duplicate entry
            // 재시도 필요 (새로운 트랜잭션에서 다시 시도하면 이미 생성된 레코드를 찾을 수 있음)
            log.debug("Duplicate entry 감지, 재시도 필요: noteId={}, memberId={}", noteId, memberId);
            throw new RuntimeException("Concurrent creation detected, retry needed", e);
        }
    }

    /**
     * Principal에서 memberId를 추출하는 헬퍼 메서드
     *
     * <p>Principal.getName()은 이메일을 반환할 수 있으므로,
     * PrincipalDetails에서 직접 member ID를 추출합니다.
     *
     * @param principal WebSocket Principal 객체
     * @return member ID
     * @throws IllegalArgumentException Principal이 올바른 형식이 아닌 경우
     */
    private Long extractMemberId(Principal principal) {
        if (!(principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken)) {
            log.error("인증 타입이 잘못됨: {}", principal.getClass().getSimpleName());
            throw new IllegalArgumentException("Invalid authentication type");
        }

        Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
        if (!(principalObj instanceof PrincipalDetails)) {
            log.error("Principal 타입이 잘못됨: {}", principalObj.getClass().getSimpleName());
            throw new IllegalArgumentException("Invalid principal type");
        }

        return ((PrincipalDetails) principalObj).getMember().getId();
    }
}
