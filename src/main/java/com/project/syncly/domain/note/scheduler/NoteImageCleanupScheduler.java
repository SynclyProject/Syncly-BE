package com.project.syncly.domain.note.scheduler;

import com.project.syncly.domain.note.service.NoteImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 노트 이미지 정리 스케줄러
 *
 * <p>만료된 PENDING 상태의 이미지를 주기적으로 정리합니다.
 *
 * <p><b>정리 대상:</b>
 * <ul>
 *   <li>uploadStatus = PENDING</li>
 *   <li>expiresAt < 현재 시간</li>
 * </ul>
 *
 * <p><b>실행 주기:</b> 10분마다 (cron: "0 *\/10 * * * *")
 *
 * <p><b>처리 내용:</b>
 * <ol>
 *   <li>만료된 PENDING 이미지 조회</li>
 *   <li>S3 객체 삭제</li>
 *   <li>DB 레코드 삭제</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoteImageCleanupScheduler {

    private final NoteImageService noteImageService;

    /**
     * 만료된 PENDING 이미지 정리
     *
     * <p>10분마다 실행되어 다음 작업을 수행합니다:
     * <ul>
     *   <li>PENDING 상태이고 expiresAt이 지난 이미지 조회</li>
     *   <li>S3에서 객체 삭제</li>
     *   <li>DB에서 레코드 삭제</li>
     * </ul>
     *
     * <p><b>왜 필요한가?</b><br>
     * 클라이언트가 Presigned URL을 받았지만 실제로 업로드하지 않은 경우,
     * DB에 PENDING 상태의 레코드가 남게 됩니다. 이를 주기적으로 정리하여
     * 불필요한 데이터 누적을 방지합니다.
     *
     * <p><b>실행 시간:</b> 매 시 0분, 10분, 20분, 30분, 40분, 50분
     */
    @Scheduled(cron = "0 */10 * * * *") // 10분마다 실행
    public void cleanupExpiredImages() {
        log.debug("만료된 노트 이미지 정리 스케줄러 시작");

        try {
            int deletedCount = noteImageService.cleanupExpiredImages();

            if (deletedCount > 0) {
                log.info("만료된 노트 이미지 정리 완료: {}개 삭제", deletedCount);
            } else {
                log.debug("정리할 만료 이미지 없음");
            }
        } catch (Exception e) {
            log.error("만료된 노트 이미지 정리 중 오류 발생", e);
        }
    }
}
