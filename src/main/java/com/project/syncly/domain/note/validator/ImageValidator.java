package com.project.syncly.domain.note.validator;

import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 이미지 파일 유효성 검증 컴포넌트
 *
 * <p>S3에 업로드되는 이미지의 타입, 크기, 파일명 등을 검증합니다.
 */
@Slf4j
@Component
public class ImageValidator {

    // 허용되는 이미지 확장자
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    ));

    // 허용되는 MIME 타입
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/x-png"  // 구형 브라우저 호환성
    ));

    // 파일 크기 제한 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 파일명 최대 길이
    private static final int MAX_FILENAME_LENGTH = 255;

    // 파일명 유효성 패턴 (XSS, 경로 탐색 방지)
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]+$"
    );

    /**
     * 이미지 파일 전체 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>파일명이 유효한가?</li>
     *   <li>MIME 타입이 이미지 형식인가?</li>
     *   <li>파일 크기가 제한을 초과하지 않는가?</li>
     *   <li>확장자가 허용 목록에 있는가?</li>
     * </ul>
     *
     * @param filename 파일명
     * @param contentType MIME 타입 (예: "image/jpeg")
     * @param fileSize 파일 크기 (바이트)
     * @throws NoteException 이미지가 유효하지 않음
     */
    public void validateImageFile(String filename, String contentType, long fileSize) {
        validateFilename(filename);
        validateContentType(contentType);
        validateFileSize(fileSize);
        validateExtension(filename);

        log.debug("이미지 파일 검증 성공: filename={}, contentType={}, fileSize={}",
                filename, contentType, fileSize);
    }

    /**
     * 파일명 유효성 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>파일명이 null/empty가 아닌가?</li>
     *   <li>파일명 길이가 255자 이하인가?</li>
     *   <li>파일명에 XSS/경로 탐색 위험 문자가 없는가?</li>
     * </ul>
     *
     * @param filename 검증할 파일명
     * @throws NoteException 파일명이 유효하지 않음
     */
    private void validateFilename(String filename) {
        // null/empty 체크
        if (filename == null || filename.isBlank()) {
            log.warn("빈 파일명으로 이미지 업로드 시도");
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE, "파일명이 비어있습니다");
        }

        String trimmedFilename = filename.trim();

        // 길이 체크
        if (trimmedFilename.length() > MAX_FILENAME_LENGTH) {
            log.warn("파일명이 너무 깨미: length={}", trimmedFilename.length());
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    String.format("파일명은 %d자 이하여야 합니다", MAX_FILENAME_LENGTH));
        }

        // 위험 문자 체크 (경로 탐색, XSS 방지)
        if (trimmedFilename.contains("..") || trimmedFilename.contains("/") ||
                trimmedFilename.contains("\\") || trimmedFilename.contains(":")) {
            log.warn("파일명에 위험 문자 포함: filename={}", trimmedFilename);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    "파일명에 유효하지 않은 문자가 포함되어 있습니다");
        }

        // 패턴 검증
        if (!VALID_FILENAME_PATTERN.matcher(trimmedFilename).matches()) {
            log.warn("파일명이 패턴을 위반: filename={}", trimmedFilename);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    "파일명은 영문, 숫자, 점(.), 하이픈(-), 언더스코어(_)만 포함 가능합니다");
        }
    }

    /**
     * Content-Type (MIME 타입) 검증
     *
     * <p>다음을 확인합니다:
     * <ul>
     *   <li>MIME 타입이 null/empty가 아닌가?</li>
     *   <li>MIME 타입이 "image/"로 시작하는가?</li>
     *   <li>MIME 타입이 허용 목록에 있는가?</li>
     * </ul>
     *
     * @param contentType 검증할 MIME 타입
     * @throws NoteException MIME 타입이 이미지가 아님
     */
    private void validateContentType(String contentType) {
        // null/empty 체크
        if (contentType == null || contentType.isBlank()) {
            log.warn("null/empty Content-Type");
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    "Content-Type이 지정되지 않았습니다");
        }

        String lowerContentType = contentType.toLowerCase().trim();

        // 기본 "image/" 형식 체크
        if (!lowerContentType.startsWith("image/")) {
            log.warn("이미지가 아닌 파일: contentType={}", contentType);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    String.format("지원하지 않는 파일 형식입니다: %s", contentType));
        }

        // 허용 목록 체크
        if (!ALLOWED_MIME_TYPES.contains(lowerContentType)) {
            log.warn("허용되지 않는 이미지 형식: contentType={}", contentType);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    String.format("지원하는 이미지 형식: %s", String.join(", ", ALLOWED_MIME_TYPES)));
        }
    }

    /**
     * 파일 크기 검증
     *
     * <p>파일 크기가 10MB를 초과하지 않는지 확인합니다.
     *
     * @param fileSize 파일 크기 (바이트)
     * @throws NoteException 파일이 너무 큼
     */
    private void validateFileSize(long fileSize) {
        if (fileSize <= 0) {
            log.warn("잘못된 파일 크기: fileSize={}", fileSize);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    "파일 크기가 0보다 커야 합니다");
        }

        if (fileSize > MAX_FILE_SIZE) {
            log.warn("파일이 너무 큼: fileSize={} (max={})", fileSize, MAX_FILE_SIZE);
            throw new NoteException(NoteErrorCode.IMAGE_SIZE_EXCEEDED,
                    String.format("파일 크기는 %dMB 이하여야 합니다", MAX_FILE_SIZE / (1024 * 1024)));
        }
    }

    /**
     * 파일 확장자 검증
     *
     * <p>파일명의 확장자가 허용 목록에 있는지 확인합니다.
     *
     * @param filename 검증할 파일명
     * @throws NoteException 확장자가 허용되지 않음
     */
    private void validateExtension(String filename) {
        if (!filename.contains(".")) {
            log.warn("확장자가 없는 파일: filename={}", filename);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    "파일에 확장자가 없습니다");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1)
                .toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("허용되지 않는 확장자: extension={}", extension);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    String.format("지원하는 확장자: %s", String.join(", ", ALLOWED_EXTENSIONS)));
        }
    }

    /**
     * 이미지 크기 범위 검증 (선택)
     *
     * <p>이미지가 너무 작지 않은지 확인합니다.
     * 예: 1x1 픽셀 이미지 스팸 방지
     *
     * @param fileSize 파일 크기 (바이트)
     * @throws NoteException 파일이 너무 작음
     */
    public void validateMinimumFileSize(long fileSize) {
        long MIN_FILE_SIZE = 100; // 최소 100 바이트

        if (fileSize < MIN_FILE_SIZE) {
            log.warn("파일이 너무 작음: fileSize={}", fileSize);
            throw new NoteException(NoteErrorCode.INVALID_IMAGE_TYPE,
                    String.format("파일 크기는 최소 %d 바이트 이상이어야 합니다", MIN_FILE_SIZE));
        }
    }

    /**
     * 허용되는 확장자 목록 반환 (클라이언트 안내용)
     */
    public Set<String> getAllowedExtensions() {
        return new HashSet<>(ALLOWED_EXTENSIONS);
    }

    /**
     * 최대 파일 크기 반환 (클라이언트 안내용)
     */
    public long getMaxFileSizeMB() {
        return MAX_FILE_SIZE / (1024 * 1024);
    }
}
