package com.project.syncly.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class WorkspaceRequestDto {
    @Schema(description = "팀 워크스페이스 생성 요청 DTO")
    public record CreateTeamWorkspaceRequestDto(
            @NotNull String workspaceName
    ) {
    }
}
