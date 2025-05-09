package com.project.syncly.domain.workspace.converter;

import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import org.hibernate.jdbc.Work;

public class WorkspaceConverter {

    public static Workspace toPersonalWorkspace(Long memberId) {
        return Workspace.builder()
                .workspaceName("회원 " + memberId + "의 워크스페이스")
                .workspaceType(WorkspaceType.PERSONAL)
                .build();
    }

    public static Workspace toTeamWorkspace(String workspaceName) {
        return Workspace.builder()
                .workspaceName(workspaceName)
                .workspaceType(WorkspaceType.TEAM)
                .build();
    }

    public static WorkspaceResponseDto.CreateWorkspaceResponseDto toWorkspaceResponse(Workspace workspace) {
        return WorkspaceResponseDto.CreateWorkspaceResponseDto.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getWorkspaceName())
                .workspaceType(workspace.getWorkspaceType().name())
                .createdAt(workspace.getCreatedAt())
                .build();
    }
}
