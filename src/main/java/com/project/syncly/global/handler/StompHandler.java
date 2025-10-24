package com.project.syncly.global.handler;

import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.jwt.JwtProvider;
import com.project.syncly.global.jwt.PrincipalDetails;
import com.project.syncly.global.jwt.exception.JwtErrorCode;
import com.project.syncly.global.jwt.exception.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP WebSocket 메시지를 가로채서 JWT 인증 및 권한 검증을 수행하는 인터셉터
 *
 * <p>주요 기능:
 * <ul>
 *   <li>CONNECT: JWT 토큰 검증 및 인증 정보 설정</li>
 *   <li>SUBSCRIBE/SEND: 노트 접근 권한 검증 (워크스페이스 멤버십 확인)</li>
 * </ul>
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class StompHandler implements ChannelInterceptor {
    private final JwtProvider jwtProvider;
    private final NoteRepository noteRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    // 노트 관련 destination 패턴: /app/notes/{noteId}/... 또는 /topic/notes/{noteId}/...
    private static final Pattern NOTE_DESTINATION_PATTERN = Pattern.compile("^/(app|topic|queue)/notes/(\\d+)/");

    /**
     * WebSocket 메시지 전송 전 인터셉트
     *
     * @param message 전송할 메시지
     * @param channel 메시지 채널
     * @return 처리된 메시지 (null 반환 시 메시지 전송 중단)
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        try {
            // CONNECT: JWT 토큰 검증 및 인증 정보 설정
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                handleConnect(accessor);
            }
            // SUBSCRIBE, SEND: 노트 접근 권한 검증
            else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
                     StompCommand.SEND.equals(accessor.getCommand())) {
                handleNoteAccessAuthorization(accessor);
            }
        } catch (Exception ex) {
            log.error("StompHandler 예외 발생: command={}, message={}",
                    accessor.getCommand(), ex.getMessage(), ex);
            throw ex;
        }

        return message;
    }

    /**
     * CONNECT 명령 처리: JWT 토큰 검증
     *
     * @param accessor STOMP 헤더 접근자
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        try {
            String rawToken = accessor.getFirstNativeHeader("Authorization");

            if (rawToken == null || !rawToken.startsWith("Bearer ")) {
                log.warn("CONNECT 요청 중 토큰이 없거나 형식이 잘못됨");
                throw new JwtException(JwtErrorCode.INVALID_TOKEN);
            }

            // "Bearer " 제거
            String token = rawToken.substring(7);

            if (!jwtProvider.isValidToken(token)) {
                log.warn("CONNECT 요청 중 토큰이 유효하지 않음");
                throw new JwtException(JwtErrorCode.INVALID_TOKEN);
            }

            Authentication authentication = jwtProvider.getAuthentication(token);
            log.info("CONNECT JWT 파싱 성공: authType={}, principalType={}, authName={}",
                    authentication.getClass().getSimpleName(),
                    authentication.getPrincipal().getClass().getSimpleName(),
                    authentication.getName());

            accessor.setUser(authentication);
            log.info("CONNECT accessor에 user 설정 완료: getName={}", accessor.getUser().getName());

            log.info("WebSocket CONNECT 성공: user={}", authentication.getName());
        } catch (JwtException ex) {
            log.error("CONNECT JWT 예외: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("CONNECT 예외: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * SUBSCRIBE/SEND 명령 처리: 노트 접근 권한 검증
     *
     * <p>destination이 /app/notes/{noteId}/... 또는 /topic/notes/{noteId}/... 형식인 경우
     * 해당 노트가 속한 워크스페이스의 멤버인지 확인합니다.
     *
     * @param accessor STOMP 헤더 접근자
     * @throws NoteException 노트가 존재하지 않거나 접근 권한이 없는 경우
     */
    private void handleNoteAccessAuthorization(StompHeaderAccessor accessor) {
        try {
            String destination = accessor.getDestination();
            if (destination == null) {
                log.debug("SUBSCRIBE/SEND destination이 null, 통과");
                return;
            }

            log.info("SUBSCRIBE/SEND 처리: destination={}", destination);

            // 노트 관련 destination인지 확인
            Matcher matcher = NOTE_DESTINATION_PATTERN.matcher(destination);
            if (!matcher.find()) {
                log.debug("노트 관련 아닌 destination, 통과: {}", destination);
                return; // 노트 관련 아닌 destination은 통과
            }

            Long noteId = Long.parseLong(matcher.group(2));

            // PrincipalDetails에서 직접 member ID를 가져오기
            // accessor.getUser().getName()은 이메일을 반환할 수 있으므로 PrincipalDetails에서 직접 접근
            java.security.Principal userPrincipal = accessor.getUser();
            if (!(userPrincipal instanceof UsernamePasswordAuthenticationToken)) {
                log.error("인증 타입이 잘못됨: {}", userPrincipal.getClass().getSimpleName());
                throw new IllegalArgumentException("Invalid authentication type");
            }

            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) userPrincipal;
            Object principal = auth.getPrincipal();
            if (!(principal instanceof PrincipalDetails)) {
                log.error("Principal 타입이 잘못됨: {}", principal.getClass().getSimpleName());
                throw new IllegalArgumentException("Invalid principal type");
            }

            Long memberId = ((PrincipalDetails) principal).getMember().getId();
            log.info("PrincipalDetails에서 memberId 추출: {}", memberId);

            log.info("노트 접근 권한 검증 시작: noteId={}, memberId={}", noteId, memberId);

            // 1. 노트가 존재하는지 확인 (workspace와 creator를 eager loading)
            var note = noteRepository.findByIdAndNotDeleted(noteId)
                    .orElseThrow(() -> {
                        log.error("노트를 찾을 수 없음: noteId={}", noteId);
                        return new NoteException(NoteErrorCode.NOTE_NOT_FOUND);
                    });

            log.info("노트 조회 성공: noteId={}, workspaceId={}", noteId, note.getWorkspace().getId());

            Long workspaceId = note.getWorkspace().getId();

            // 2. 사용자가 해당 워크스페이스의 멤버인지 확인
            boolean isMember = workspaceMemberRepository
                    .existsByWorkspaceIdAndMemberId(workspaceId, memberId);

            if (!isMember) {
                log.warn("WebSocket 접근 거부: noteId={}, memberId={}, workspaceId={}",
                        noteId, memberId, workspaceId);
                throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
            }

            log.info("WebSocket 접근 허가: noteId={}, memberId={}, destination={}",
                    noteId, memberId, destination);
        } catch (NoteException ex) {
            log.error("SUBSCRIBE/SEND NoteException: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("SUBSCRIBE/SEND 예외: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}