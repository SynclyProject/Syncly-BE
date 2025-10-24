package com.project.syncly.domain.note.annotation;

import java.lang.annotation.*;

/**
 * 노트 접근 권한 검증 어노테이션
 *
 * <p>컨트롤러 메서드에 이 어노테이션을 붙이면,
 * NoteAccessAspect가 메서드 실행 전에 권한을 검증합니다.
 *
 * <p>사용 예:
 * <pre>
 * {@literal @}PostMapping("/{noteId}/edit")
 * {@literal @}NoteAccess(level = AccessLevel.WRITE)
 * public void editNote({@literal @}PathVariable Long noteId, ...) {
 *     // 권한이 확인된 후 실행됨
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoteAccess {

    /**
     * 필요한 접근 권한 레벨
     *
     * <p>READ: 노트 조회만 가능
     * <p>WRITE: 노트 편집 가능
     * <p>DELETE: 노트 삭제 가능
     */
    AccessLevel level() default AccessLevel.READ;

    /**
     * 노트 ID 파라미터 이름
     *
     * <p>기본값은 "noteId"입니다.
     * 다른 이름이면 여기서 지정할 수 있습니다.
     *
     * <p>예: @NoteAccess(paramName = "id")
     */
    String paramName() default "noteId";

    /**
     * 워크스페이스 ID 파라미터 이름
     *
     * <p>기본값은 "workspaceId"입니다.
     */
    String workspaceParamName() default "workspaceId";

    /**
     * 접근 권한 레벨 열거형
     */
    enum AccessLevel {
        /**
         * 읽기: 노트 조회만 가능
         */
        READ("노트 조회"),

        /**
         * 쓰기: 노트 편집 가능
         */
        WRITE("노트 편집"),

        /**
         * 삭제: 노트 삭제 가능 (작성자만)
         */
        DELETE("노트 삭제");

        private final String description;

        AccessLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
