package com.project.syncly.domain.note.aspect;

import com.project.syncly.domain.note.annotation.NoteAccess;
import com.project.syncly.domain.note.annotation.NoteAccess.AccessLevel;
import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.note.validator.NoteValidator;
import com.project.syncly.global.jwt.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 노트 접근 권한 검증 AOP
 *
 * <p>{@link NoteAccess} 어노테이션이 붙은 메서드 실행 전에
 * 요청한 사용자의 권한을 검증합니다.
 *
 * <p>검증 항목:
 * <ul>
 *   <li>사용자 인증 확인</li>
 *   <li>워크스페이스 멤버십 확인</li>
 *   <li>노트 존재 여부 확인</li>
 *   <li>노트 삭제 여부 확인</li>
 *   <li>DELETE 권한 필요 시 작성자 확인</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NoteAccessAspect {

    private final NoteRepository noteRepository;
    private final NoteValidator noteValidator;

    /**
     * @NoteAccess 어노테이션이 붙은 메서드 실행 전 권한 검증
     *
     * @param joinPoint 프록시 정보
     * @param noteAccess 어노테이션 정보
     * @return 메서드 실행 결과
     * @throws Throwable 메서드 실행 중 발생한 예외
     */
    @Around("@annotation(noteAccess)")
    public Object validateNoteAccess(ProceedingJoinPoint joinPoint, NoteAccess noteAccess) throws Throwable {
        log.debug("NoteAccess 권한 검증 시작: level={}", noteAccess.level());

        try {
            // 1. 현재 사용자 정보 조회
            Long memberId = getCurrentMemberId();

            // 2. 메서드 파라미터에서 noteId, workspaceId 추출
            Long noteId = extractNoteId(joinPoint, noteAccess.paramName());
            Long workspaceId = extractWorkspaceId(joinPoint, noteAccess.workspaceParamName());

            log.debug("권한 검증: memberId={}, noteId={}, workspaceId={}, accessLevel={}",
                    memberId, noteId, workspaceId, noteAccess.level());

            // 3. 기본 접근 권한 검증 (노트 존재, 워크스페이스 멤버십 등)
            Note note = noteValidator.validateNoteAccess(noteId, workspaceId, memberId);

            // 4. 접근 레벨별 추가 권한 검증
            validateAccessLevel(note, memberId, noteAccess.level());

            log.debug("권한 검증 성공: noteId={}, memberId={}, accessLevel={}",
                    noteId, memberId, noteAccess.level());

            // 5. 메서드 실행
            return joinPoint.proceed();

        } catch (NoteException e) {
            log.warn("노트 접근 권한 검증 실패: {}", e.getCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("권한 검증 중 예외 발생", e);
            throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }
    }

    /**
     * 현재 인증된 사용자의 memberId 조회
     *
     * @return 사용자의 memberId
     * @throws NoteException 인증되지 않은 사용자
     */
    private Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증되지 않은 사용자가 접근 시도");
            throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }

        try {
            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
            return Long.valueOf(principalDetails.getName());
        } catch (Exception e) {
            log.error("사용자 정보 추출 실패", e);
            throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }
    }

    /**
     * 메서드 파라미터에서 noteId 추출
     *
     * @param joinPoint 프록시 정보
     * @param paramName 파라미터 이름
     * @return 추출된 noteId
     * @throws NoteException noteId를 찾을 수 없음
     */
    private Long extractNoteId(ProceedingJoinPoint joinPoint, String paramName) {
        return extractLongParameter(joinPoint, paramName, "noteId");
    }

    /**
     * 메서드 파라미터에서 workspaceId 추출
     *
     * @param joinPoint 프록시 정보
     * @param paramName 파라미터 이름
     * @return 추출된 workspaceId
     * @throws NoteException workspaceId를 찾을 수 없음
     */
    private Long extractWorkspaceId(ProceedingJoinPoint joinPoint, String paramName) {
        return extractLongParameter(joinPoint, paramName, "workspaceId");
    }

    /**
     * 메서드 파라미터에서 Long 타입 파라미터 추출
     *
     * @param joinPoint 프록시 정보
     * @param paramName 파라미터 이름
     * @param paramType 파라미터 용도 (로깅용)
     * @return 추출된 파라미터 값
     * @throws NoteException 파라미터를 찾을 수 없음
     */
    private Long extractLongParameter(ProceedingJoinPoint joinPoint, String paramName, String paramType) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName)) {
                Object value = args[i];
                if (value instanceof Long) {
                    return (Long) value;
                } else if (value instanceof String) {
                    try {
                        return Long.parseLong((String) value);
                    } catch (NumberFormatException e) {
                        log.error("{} 파라미터 파싱 실패: value={}", paramType, value);
                        throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
                    }
                } else if (value instanceof Integer) {
                    return ((Integer) value).longValue();
                }
            }
        }

        log.error("{} 파라미터를 찾을 수 없음: paramName={}", paramType, paramName);
        throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED,
                String.format("%s 파라미터를 찾을 수 없습니다", paramType));
    }

    /**
     * 접근 레벨별 추가 권한 검증
     *
     * @param note 대상 노트
     * @param memberId 요청 사용자 ID
     * @param accessLevel 필요한 접근 레벨
     * @throws NoteException 권한 없음
     */
    private void validateAccessLevel(Note note, Long memberId, AccessLevel accessLevel) {
        switch (accessLevel) {
            case READ:
                // 모든 워크스페이스 멤버는 읽기 가능
                log.debug("READ 권한 검증 완료: 모든 멤버 접근 가능");
                break;

            case WRITE:
                // WRITE 권한은 기존 READ 권한 검증으로 충분
                log.debug("WRITE 권한 검증 완료: 워크스페이스 멤버 편집 가능");
                break;

            case DELETE:
                // DELETE 권한은 작성자만 가능
                noteValidator.validateNoteCreator(note, memberId);
                log.debug("DELETE 권한 검증 완료: 작성자만 삭제 가능");
                break;

            default:
                log.error("알 수 없는 접근 레벨: {}", accessLevel);
                throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }
    }
}
