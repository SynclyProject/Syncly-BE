package com.project.syncly.domain.note.entity;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE note SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Note extends BaseTimeDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Member creator;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "last_modified_at", nullable = false)
    private LocalDateTime lastModifiedAt;

    /**
     * 노트 제목 업데이트
     */
    public void updateTitle(String title) {
        this.title = title;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * 노트 내용 업데이트
     */
    public void updateContent(String content) {
        this.content = content;
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * 소프트 삭제 시 lastModifiedAt도 갱신
     */
    @Override
    public void markAsDeleted() {
        super.markAsDeleted();
        this.lastModifiedAt = LocalDateTime.now();
    }

    /**
     * 생성 시 lastModifiedAt 초기화
     */
    @PrePersist
    public void prePersist() {
        if (this.lastModifiedAt == null) {
            this.lastModifiedAt = LocalDateTime.now();
        }
    }
}
