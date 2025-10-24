package com.project.syncly.domain.note.entity;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "note_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoteImage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private Member uploader;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 업로드 상태: PENDING(대기), COMPLETED(완료), FAILED(실패)
     */
    @Column(name = "upload_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    /**
     * Presigned URL 만료 시간
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 파일 URL 업데이트 (업로드 완료 후)
     */
    public void updateFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    /**
     * 업로드 완료 상태로 변경
     */
    public void markAsCompleted(String fileUrl) {
        this.fileUrl = fileUrl;
        this.uploadStatus = UploadStatus.COMPLETED;
    }

    /**
     * 업로드 실패 상태로 변경
     */
    public void markAsFailed() {
        this.uploadStatus = UploadStatus.FAILED;
    }

    /**
     * 업로드 상태 Enum
     */
    public enum UploadStatus {
        PENDING,    // 업로드 대기 중
        COMPLETED,  // 업로드 완료
        FAILED      // 업로드 실패
    }
}
