package com.project.syncly.domain.chat.entity;

import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "chat_message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ws_msgid", columnNames = {"workspace_id","msg_id"}),
                @UniqueConstraint(name = "uq_ws_seq",   columnNames = {"workspace_id","seq"})
        },
        indexes = {
                @Index(name = "idx_ws_created", columnList = "workspace_id, created_at")
        })
public class ChatMessage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private WorkspaceMember sender;

    @Column(name = "msg_id", nullable = false, length = 36)
    private String msgId;

    @Column(name = "seq", nullable = false)
    private Long seq;

    @Lob
    @Column(name = "chat_message", nullable = false, columnDefinition = "TEXT")
    private String content;
}

