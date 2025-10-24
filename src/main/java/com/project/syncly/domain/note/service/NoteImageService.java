package com.project.syncly.domain.note.service;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.note.dto.NoteImageDto;
import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.entity.NoteImage;
import com.project.syncly.domain.note.exception.NoteErrorCode;
import com.project.syncly.domain.note.exception.NoteException;
import com.project.syncly.domain.note.repository.NoteImageRepository;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;
import com.project.syncly.domain.s3.enums.FileMimeType;
import com.project.syncly.domain.s3.service.S3Service;
import com.project.syncly.domain.s3.util.S3Util;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 노트 이미지 업로드 서비스
 *
 * <p>S3 Presigned URL을 활용한 이미지 업로드 기능을 제공합니다.
 *
 * <p><b>업로드 플로우:</b>
 * <ol>
 *   <li>클라이언트: POST /presigned 요청 → uploadUrl, imageId 받음</li>
 *   <li>클라이언트: uploadUrl로 PUT 요청 (파일 직접 업로드)</li>
 *   <li>클라이언트: POST /images/{imageId}/confirm → imageUrl 받음</li>
 *   <li>클라이언트: imageUrl을 마크다운에 삽입</li>
 *   <li>클라이언트: WebSocket EDIT 메시지로 content 업데이트</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteImageService {

    private final NoteRepository noteRepository;
    private final NoteImageRepository noteImageRepository;
    private final MemberRepository memberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final S3Service s3Service;
    private final S3Util s3Util;

    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;

    /**
     * Presigned URL 유효 기간 (분)
     */
    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 5;

    /**
     * 이미지 업로드용 Presigned URL 생성
     *
     * <p>검증 항목:
     * <ul>
     *   <li>노트 존재 여부 및 워크스페이스 멤버 권한 확인</li>
     *   <li>파일 타입 검증 (image/* 형식)</li>
     *   <li>파일 크기 제한 (최대 10MB)</li>
     *   <li>파일명 sanitize (특수문자 제거)</li>
     * </ul>
     *
     * @param noteId 노트 ID
     * @param memberId 업로드하는 Member ID
     * @param request Presigned URL 요청 (filename, contentType, fileSize)
     * @return Presigned URL 응답 (uploadUrl, imageId, objectKey, expiresAt)
     * @throws NoteException 노트를 찾을 수 없거나 권한이 없는 경우
     * @throws NoteException 파일 타입이 이미지가 아니거나 크기가 초과된 경우
     */
    @Transactional
    public NoteImageDto.PresignedUrlResponse generateUploadUrl(
            Long noteId,
            Long memberId,
            NoteImageDto.PresignedUrlRequest request
    ) {
        log.info("Presigned URL 생성 시작: noteId={}, memberId={}, filename={}",
                noteId, memberId, request.filename());

        // 1. 노트 존재 및 권한 검증
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOTE_NOT_FOUND));

        // 워크스페이스 멤버 권한 확인
        WorkspaceMember workspaceMember = workspaceMemberRepository
                .findByWorkspaceIdAndMemberId(note.getWorkspace().getId(), memberId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.NOT_WORKSPACE_MEMBER));

        // 2. 파일 크기 검증
        request.validateFileSize();

        // 3. 파일명 sanitize
        String sanitizedFilename = request.sanitizeFilename();

        // 4. S3Service를 통해 Presigned URL 생성 (기존 인프라 재사용)
        FileMimeType mimeType = FileMimeType.fromKey(request.contentType());
        S3RequestDTO.NoteImageUploadPreSignedUrl s3Request =
                new S3RequestDTO.NoteImageUploadPreSignedUrl(noteId, sanitizedFilename, mimeType);

        S3ResponseDTO.PreSignedUrl s3Response = s3Service.generatePresignedPutUrl(memberId, s3Request);

        // 5. NoteImage 엔티티 생성 (PENDING 상태)
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PRESIGNED_URL_EXPIRY_MINUTES);

        NoteImage noteImage = NoteImage.builder()
                .note(note)
                .uploader(member)
                .objectKey(s3Response.objectKey())
                .originalFilename(sanitizedFilename)
                .contentType(request.contentType())
                .fileSize(request.fileSize())
                .uploadStatus(NoteImage.UploadStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        noteImage = noteImageRepository.save(noteImage);

        log.info("Presigned URL 생성 완료: imageId={}, objectKey={}", noteImage.getId(), s3Response.objectKey());

        return new NoteImageDto.PresignedUrlResponse(
                s3Response.uploadUrl(),
                noteImage.getId(),
                s3Response.objectKey(),
                expiresAt
        );
    }

    /**
     * 이미지 업로드 완료 확인 및 공개 URL 생성
     *
     * <p>S3에 객체가 실제로 업로드되었는지 확인하고, COMPLETED 상태로 변경합니다.
     * CloudFront URL 또는 S3 공개 URL을 생성하여 반환합니다.
     *
     * @param noteId 노트 ID
     * @param imageId 이미지 ID
     * @param memberId 업로드한 Member ID
     * @return 이미지 URL 응답 (imageUrl, markdownSyntax)
     * @throws NoteException 이미지를 찾을 수 없거나 업로드가 완료되지 않은 경우
     */
    @Transactional
    public NoteImageDto.ImageUrlResponse confirmUpload(Long noteId, Long imageId, Long memberId) {
        log.info("이미지 업로드 확인 시작: noteId={}, imageId={}, memberId={}", noteId, imageId, memberId);

        // 1. NoteImage 조회
        NoteImage noteImage = noteImageRepository.findByIdAndNoteId(imageId, noteId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.IMAGE_NOT_FOUND));

        // 2. 업로더 확인
        if (!noteImage.getUploader().getId().equals(memberId)) {
            throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }

        // 3. 만료 시간 검증
        if (noteImage.getExpiresAt() != null && LocalDateTime.now().isAfter(noteImage.getExpiresAt())) {
            noteImage.markAsFailed();
            noteImageRepository.save(noteImage);
            throw new NoteException(NoteErrorCode.IMAGE_UPLOAD_FAILED, "Presigned URL이 만료되었습니다");
        }

        // 4. S3 객체 존재 여부 확인
        if (!s3Util.objectExists(noteImage.getObjectKey())) {
            log.warn("S3 객체가 존재하지 않음: objectKey={}", noteImage.getObjectKey());
            noteImage.markAsFailed();
            noteImageRepository.save(noteImage);
            throw new NoteException(NoteErrorCode.IMAGE_UPLOAD_FAILED, "이미지 업로드가 완료되지 않았습니다");
        }

        // 5. CloudFront URL 생성
        String imageUrl = String.format("https://%s/%s", cloudFrontDomain, noteImage.getObjectKey());

        // 6. COMPLETED 상태로 변경
        noteImage.markAsCompleted(imageUrl);
        noteImageRepository.save(noteImage);

        log.info("이미지 업로드 확인 완료: imageId={}, imageUrl={}", imageId, imageUrl);

        return NoteImageDto.ImageUrlResponse.from(imageUrl, noteImage.getOriginalFilename());
    }

    /**
     * 이미지 삭제
     *
     * <p>S3 객체와 DB 레코드를 모두 삭제합니다.
     *
     * @param noteId 노트 ID
     * @param imageId 이미지 ID
     * @param memberId 삭제 요청한 Member ID
     * @return 삭제 응답
     * @throws NoteException 이미지를 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public NoteImageDto.ImageDeleteResponse deleteImage(Long noteId, Long imageId, Long memberId) {
        log.info("이미지 삭제 시작: noteId={}, imageId={}, memberId={}", noteId, imageId, memberId);

        // 1. NoteImage 조회
        NoteImage noteImage = noteImageRepository.findByIdAndNoteId(imageId, noteId)
                .orElseThrow(() -> new NoteException(NoteErrorCode.IMAGE_NOT_FOUND));

        // 2. 권한 확인 (업로더 본인 또는 노트 작성자)
        Note note = noteImage.getNote();
        if (!noteImage.getUploader().getId().equals(memberId)
                && !note.getCreator().getId().equals(memberId)) {
            throw new NoteException(NoteErrorCode.NOTE_ACCESS_DENIED);
        }

        // 3. S3 객체 삭제
        try {
            s3Util.delete(noteImage.getObjectKey());
            log.debug("S3 객체 삭제 완료: objectKey={}", noteImage.getObjectKey());
        } catch (Exception e) {
            log.warn("S3 객체 삭제 실패 (계속 진행): objectKey={}, error={}",
                    noteImage.getObjectKey(), e.getMessage());
        }

        // 4. DB 레코드 삭제
        noteImageRepository.delete(noteImage);

        log.info("이미지 삭제 완료: imageId={}", imageId);

        return NoteImageDto.ImageDeleteResponse.of(imageId);
    }

    /**
     * 만료된 PENDING 이미지 정리 (스케줄러용)
     *
     * <p>PENDING 상태이고 expiresAt이 지난 이미지들을 S3와 DB에서 삭제합니다.
     *
     * @return 정리된 이미지 개수
     */
    @Transactional
    public int cleanupExpiredImages() {
        log.debug("만료된 PENDING 이미지 정리 시작");

        LocalDateTime now = LocalDateTime.now();
        List<NoteImage> expiredImages = noteImageRepository.findExpiredPendingImages(now);

        if (expiredImages.isEmpty()) {
            log.debug("정리할 만료 이미지 없음");
            return 0;
        }

        log.info("만료된 이미지 {}개 발견, 정리 시작", expiredImages.size());

        int deletedCount = 0;
        for (NoteImage image : expiredImages) {
            try {
                // S3 객체 삭제
                s3Util.delete(image.getObjectKey());
                log.debug("S3 객체 삭제: objectKey={}", image.getObjectKey());

                // DB 레코드 삭제
                noteImageRepository.delete(image);

                deletedCount++;
            } catch (Exception e) {
                log.error("이미지 정리 실패: imageId={}, objectKey={}, error={}",
                        image.getId(), image.getObjectKey(), e.getMessage());
            }
        }

        log.info("만료된 이미지 정리 완료: 총 {}개 중 {}개 삭제", expiredImages.size(), deletedCount);

        return deletedCount;
    }
}
