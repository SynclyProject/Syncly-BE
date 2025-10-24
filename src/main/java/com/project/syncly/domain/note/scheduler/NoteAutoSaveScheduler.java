package com.project.syncly.domain.note.scheduler;

import com.project.syncly.domain.note.dto.NoteWebSocketDto;
import com.project.syncly.domain.note.entity.Note;
import com.project.syncly.domain.note.repository.NoteRepository;
import com.project.syncly.domain.note.service.NoteRedisService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 노트 자동 저장 스케줄러
 *
 * <p>주기적으로 Redis의 dirty 플래그가 true인 노트들을 MySQL에 저장합니다.
 *
 * <p><b>실행 주기:</b> 30초마다
 *
 * <p><b>처리 흐름:</b>
 * <ol>
 *   <li>Redis에서 모든 dirty 노트 스캔 (SCAN 사용)</li>
 *   <li>각 노트를 독립적인 트랜잭션으로 저장</li>
 *   <li>저장 성공 시 dirty 플래그 false로 변경</li>
 *   <li>WebSocket으로 저장 완료 메시지 브로드캐스트</li>
 * </ol>
 *
 * <p><b>안정성 보장:</b>
 * <ul>
 *   <li>동시 실행 방지 (AtomicBoolean 플래그)</li>
 *   <li>독립적인 트랜잭션 (한 노트 실패해도 다른 노트 저장 계속)</li>
 *   <li>Redis SCAN 사용 (KEYS 대신 - blocking 방지)</li>
 *   <li>예외 처리 및 상세 로깅</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoteAutoSaveScheduler {

    private final NoteRepository noteRepository;
    private final NoteRedisService noteRedisService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * 스케줄러 동시 실행 방지 플래그
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 저장 성공/실패 카운터 (모니터링용)
     */
    private final AtomicInteger totalSavedCount = new AtomicInteger(0);
    private final AtomicInteger totalFailedCount = new AtomicInteger(0);
    private final AtomicInteger redisErrorCount = new AtomicInteger(0);

    /**
     * OptimisticLockException 재시도 설정
     */
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 100; // 재시도 간격 (100ms)

    /**
     * 자동 저장 스케줄러 (30초마다 실행)
     *
     * <p>이전 실행이 완료되지 않았으면 현재 실행을 스킵합니다.
     * Redis 오류 발생 시 로그 및 알람을 기록합니다.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 30000) // 30초마다, 최초 30초 후 시작
    public void autoSaveNotes() {
        // 동시 실행 방지
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("이전 자동 저장이 아직 실행 중입니다. 현재 실행을 스킵합니다.");
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            log.debug("자동 저장 스케줄러 시작");

            // Redis에서 모든 dirty 노트 ID 조회
            Set<Long> dirtyNoteIds;
            try {
                dirtyNoteIds = noteRedisService.getAllDirtyNoteIds();
            } catch (RedisConnectionFailureException e) {
                log.error("Redis 연결 오류 발생: dirty 노트 조회 실패", e);
                redisErrorCount.incrementAndGet();
                meterRegistry.counter("note.autosave.redis.errors").increment();
                alertRedisError(e);
                return;
            }

            if (dirtyNoteIds.isEmpty()) {
                log.debug("저장할 dirty 노트가 없습니다");
                sample.stop(Timer.builder("note.autosave.duration")
                        .tag("status", "no_notes")
                        .register(meterRegistry));
                return;
            }

            log.info("자동 저장 시작: {}개의 dirty 노트 발견", dirtyNoteIds.size());

            int savedCount = 0;
            int failedCount = 0;

            // 각 노트를 독립적으로 저장
            for (Long noteId : dirtyNoteIds) {
                try {
                    boolean saved = saveNoteToDatabase(noteId);
                    if (saved) {
                        savedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("노트 자동 저장 중 예외 발생: noteId={}", noteId, e);
                    failedCount++;
                    meterRegistry.counter("note.autosave.failures").increment();
                }
            }

            // 통계 업데이트
            totalSavedCount.addAndGet(savedCount);
            totalFailedCount.addAndGet(failedCount);

            // 메트릭 기록
            meterRegistry.counter("note.autosave.success").increment(savedCount);
            meterRegistry.counter("note.autosave.failures").increment(failedCount);
            Gauge.builder("note.autosave.total.success", totalSavedCount::get)
                    .register(meterRegistry);
            Gauge.builder("note.autosave.total.failures", totalFailedCount::get)
                    .register(meterRegistry);

            log.info("자동 저장 완료: 성공={}, 실패={}, 총누적-성공={}, 총누적-실패={}",
                    savedCount, failedCount, totalSavedCount.get(), totalFailedCount.get());

            sample.stop(Timer.builder("note.autosave.duration")
                    .tag("status", "completed")
                    .tag("saved", String.valueOf(savedCount))
                    .tag("failed", String.valueOf(failedCount))
                    .register(meterRegistry));

        } catch (Exception e) {
            log.error("자동 저장 스케줄러 실행 중 치명적 오류 발생", e);
            meterRegistry.counter("note.autosave.critical.errors").increment();
            sample.stop(Timer.builder("note.autosave.duration")
                    .tag("status", "error")
                    .register(meterRegistry));
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * 단일 노트를 데이터베이스에 저장 (독립 트랜잭션)
     *
     * <p>각 노트는 독립적인 트랜잭션으로 처리되어,
     * 한 노트의 저장 실패가 다른 노트에 영향을 주지 않습니다.
     *
     * <p>OptimisticLockException 발생 시 최대 3회까지 재시도합니다.
     *
     * @param noteId 저장할 노트 ID
     * @return 저장 성공 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveNoteToDatabase(Long noteId) {
        return saveNoteWithRetry(noteId, 0);
    }

    /**
     * 재시도 로직을 포함한 노트 저장
     *
     * @param noteId 저장할 노트 ID
     * @param retryCount 현재 재시도 횟수
     * @return 저장 성공 여부
     */
    private boolean saveNoteWithRetry(Long noteId, int retryCount) {
        try {
            log.debug("노트 저장 시작: noteId={}, retryCount={}", noteId, retryCount);
            Timer.Sample sampleSave = Timer.start(meterRegistry);

            // 1. Redis에서 현재 상태 조회
            String content;
            try {
                content = noteRedisService.getContent(noteId);
            } catch (RedisConnectionFailureException e) {
                log.error("Redis 연결 오류: noteId={}, 저장 중단", noteId, e);
                redisErrorCount.incrementAndGet();
                meterRegistry.counter("note.autosave.redis.errors").increment();
                alertRedisError(e);
                return false;
            }

            if (content == null) {
                log.warn("Redis에 content가 없습니다: noteId={}", noteId);
                noteRedisService.clearDirty(noteId);
                sampleSave.stop(Timer.builder("note.autosave.save.time")
                        .tag("result", "no_content")
                        .register(meterRegistry));
                return false;
            }

            int revision = noteRedisService.getRevision(noteId);

            // 2. DB에서 Note 엔티티 조회
            Note note = noteRepository.findById(noteId).orElse(null);
            if (note == null) {
                log.warn("DB에 노트가 존재하지 않습니다: noteId={}", noteId);
                noteRedisService.clearDirty(noteId);
                sampleSave.stop(Timer.builder("note.autosave.save.time")
                        .tag("result", "not_found")
                        .register(meterRegistry));
                return false;
            }

            // 3. Content 업데이트
            note.updateContent(content);

            // 4. DB 저장
            noteRepository.save(note);

            // 5. Redis dirty 플래그 false로 변경
            try {
                noteRedisService.clearDirty(noteId);
            } catch (RedisConnectionFailureException e) {
                log.error("Redis dirty 플래그 삭제 실패: noteId={}", noteId, e);
                redisErrorCount.incrementAndGet();
                // 하지만 DB 저장은 성공했으므로 true 반환
            }

            log.info("노트 저장 완료: noteId={}, revision={}, contentLength={}, retryCount={}",
                    noteId, revision, content.length(), retryCount);

            // 6. WebSocket 브로드캐스트 (저장 완료 알림)
            broadcastSaveCompleted(noteId, revision);

            sampleSave.stop(Timer.builder("note.autosave.save.time")
                    .tag("result", "success")
                    .tag("retry_count", String.valueOf(retryCount))
                    .register(meterRegistry));

            return true;

        } catch (OptimisticLockingFailureException e) {
            // OptimisticLockException 재시도 로직
            log.warn("OptimisticLockException 발생: noteId={}, retryCount={}/{}",
                    noteId, retryCount, MAX_RETRY_ATTEMPTS, e);

            if (retryCount < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * (retryCount + 1)); // 점진적 지연
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("재시도 대기 중 인터럽트됨: noteId={}", noteId);
                }

                meterRegistry.counter("note.autosave.retry.attempt")
                        .increment();

                // 새로운 트랜잭션에서 재시도
                try {
                    return performRetry(noteId, retryCount + 1);
                } catch (Exception retryException) {
                    log.error("재시도 실패: noteId={}, retryCount={}", noteId, retryCount + 1, retryException);
                    return false;
                }
            } else {
                log.error("최대 재시도 횟수 초과: noteId={}, maxRetries={}", noteId, MAX_RETRY_ATTEMPTS);
                meterRegistry.counter("note.autosave.retry.failed").increment();
                return false;
            }

        } catch (Exception e) {
            log.error("노트 저장 실패: noteId={}, retryCount={}", noteId, retryCount, e);
            meterRegistry.counter("note.autosave.save.errors").increment();
            return false;
        }
    }

    /**
     * 새로운 트랜잭션에서 재시도 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean performRetry(Long noteId, int retryCount) {
        return saveNoteWithRetry(noteId, retryCount);
    }

    /**
     * WebSocket으로 저장 완료 메시지 브로드캐스트
     */
    private void broadcastSaveCompleted(Long noteId, int revision) {
        try {
            NoteWebSocketDto.SaveCompletedMessage message =
                    NoteWebSocketDto.SaveCompletedMessage.of(revision);

            messagingTemplate.convertAndSend(
                    "/topic/notes/" + noteId + "/save",
                    message
            );

            log.debug("저장 완료 메시지 브로드캐스트: noteId={}, revision={}", noteId, revision);
        } catch (Exception e) {
            // WebSocket 브로드캐스트 실패는 치명적이지 않음
            log.warn("저장 완료 메시지 브로드캐스트 실패: noteId={}", noteId, e);
        }
    }

    /**
     * 수동 저장 (API 호출용)
     *
     * <p>사용자가 명시적으로 저장 버튼을 클릭했을 때 호출됩니다.
     * 자동 저장과 동일한 로직을 사용하되, 즉시 실행됩니다.
     *
     * @param noteId 저장할 노트 ID
     * @return 저장 성공 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveNoteManually(Long noteId) {
        log.info("수동 저장 요청: noteId={}", noteId);
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            boolean result = saveNoteToDatabase(noteId);
            sample.stop(Timer.builder("note.manual.save.time")
                    .tag("result", result ? "success" : "failure")
                    .register(meterRegistry));
            return result;
        } catch (Exception e) {
            log.error("수동 저장 중 오류: noteId={}", noteId, e);
            sample.stop(Timer.builder("note.manual.save.time")
                    .tag("result", "error")
                    .register(meterRegistry));
            meterRegistry.counter("note.manual.save.errors").increment();
            return false;
        }
    }

    /**
     * 애플리케이션 시작 시 모든 dirty 노트 즉시 저장
     *
     * <p>애플리케이션이 재시작되었을 때, Redis에 남아있는
     * dirty 노트들을 즉시 DB에 저장하여 데이터 손실을 방지합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void saveAllDirtyNotesOnStartup() {
        log.info("애플리케이션 시작: 모든 dirty 노트 저장 시작");

        try {
            Set<Long> dirtyNoteIds;
            try {
                dirtyNoteIds = noteRedisService.getAllDirtyNoteIds();
            } catch (RedisConnectionFailureException e) {
                log.error("애플리케이션 시작 시 Redis 연결 오류", e);
                redisErrorCount.incrementAndGet();
                meterRegistry.counter("note.startup.redis.errors").increment();
                alertRedisError(e);
                return;
            }

            if (dirtyNoteIds.isEmpty()) {
                log.info("저장할 dirty 노트가 없습니다");
                return;
            }

            log.info("{}개의 dirty 노트 발견, 저장 시작", dirtyNoteIds.size());

            int savedCount = 0;
            int failedCount = 0;

            for (Long noteId : dirtyNoteIds) {
                try {
                    boolean saved = saveNoteToDatabase(noteId);
                    if (saved) {
                        savedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("시작 시 노트 저장 실패: noteId={}", noteId, e);
                    failedCount++;
                }
            }

            totalSavedCount.addAndGet(savedCount);
            totalFailedCount.addAndGet(failedCount);

            meterRegistry.counter("note.startup.saved").increment(savedCount);
            meterRegistry.counter("note.startup.failed").increment(failedCount);

            log.info("애플리케이션 시작 시 저장 완료: 성공={}, 실패={}, 총누적-성공={}, 총누적-실패={}",
                    savedCount, failedCount, totalSavedCount.get(), totalFailedCount.get());

        } catch (Exception e) {
            log.error("애플리케이션 시작 시 저장 중 오류 발생", e);
            meterRegistry.counter("note.startup.critical.errors").increment();
        }
    }

    /**
     * Redis 연결 오류 알람
     *
     * <p>Redis 연결에 실패했을 때 로그 및 알람을 전송합니다.
     * 현재는 로그만 기록하지만, 향후 Slack, 이메일 등으로 확장 가능합니다.
     *
     * @param exception Redis 연결 예외
     */
    private void alertRedisError(Exception exception) {
        String errorMessage = String.format(
                "[CRITICAL] Redis 연결 오류 발생: %s | 시간: %s",
                exception.getMessage(),
                LocalDateTime.now()
        );

        log.error(errorMessage, exception);

        // 향후 Slack, 이메일 등으로 확장 가능
        // TODO: SlackNotificationService, EmailService 등을 주입받아 알람 전송
        // slackNotificationService.sendAlert(errorMessage);
        // emailService.sendAlert("admin@example.com", "Redis 연결 오류", errorMessage);
    }

    /**
     * 저장 통계 조회 (모니터링용)
     */
    public String getStatistics() {
        return String.format(
                """
                === 자동 저장 통계 ===
                총 저장 성공: %d
                총 저장 실패: %d
                Redis 오류: %d
                성공률: %.2f%%
                """,
                totalSavedCount.get(),
                totalFailedCount.get(),
                redisErrorCount.get(),
                calculateSuccessRate()
        );
    }

    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        int total = totalSavedCount.get() + totalFailedCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (totalSavedCount.get() * 100.0) / total;
    }

    /**
     * 통계 초기화 (테스트용)
     */
    public void resetStatistics() {
        totalSavedCount.set(0);
        totalFailedCount.set(0);
        redisErrorCount.set(0);
        log.info("저장 통계 초기화됨");
    }
}
