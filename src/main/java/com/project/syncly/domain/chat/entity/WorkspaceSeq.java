package com.project.syncly.domain.chat.entity;

import com.project.syncly.domain.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "workspace_seq")
public class WorkspaceSeq {
    @Id
    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "current_seq", nullable = false)
    private Long currentSeq;
}