package com.project.syncly.domain.workspace.dto;

import lombok.Getter;

public class WorkspaceRequestDto {
    public record CreateTeamWorkspaceRequestDto(
            String workspaceName
    ) {
    }
}
