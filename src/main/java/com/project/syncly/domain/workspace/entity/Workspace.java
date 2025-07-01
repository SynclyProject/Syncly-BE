package com.project.syncly.domain.workspace.entity;

import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workspace")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class Workspace extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_name", length = 100, nullable = false)
    private String workspaceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_type", nullable = false)
    private WorkspaceType workspaceType;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceMember> workspaceMembers;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkspaceInvitation> workspaceInvitations;

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
}
