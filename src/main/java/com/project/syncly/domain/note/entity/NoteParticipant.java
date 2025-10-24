package com.project.syncly.domain.note.entity;

import com.project.syncly.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "note_participant",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_note_member",
            columnNames = {"note_id", "member_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoteParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_online", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isOnline = true;

    /**
     * 온라인 상태로 변경 (재입장 시)
     */
    public void setOnline() {
        this.isOnline = true;
        this.joinedAt = LocalDateTime.now();
    }

    /**
     * 오프라인 상태로 변경 (퇴장 시)
     */
    public void setOffline() {
        this.isOnline = false;
    }

    /**
     * 생성 시 joinedAt 초기화
     */
    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.isOnline == null) {
            this.isOnline = true;
        }
    }
}
