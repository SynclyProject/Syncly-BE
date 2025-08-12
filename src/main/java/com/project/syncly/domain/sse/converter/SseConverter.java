package com.project.syncly.domain.sse.converter;

import com.project.syncly.domain.sse.dto.SseResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.WorkspaceInvitation;

import java.time.LocalDateTime;
import java.util.List;

public class SseConverter {
    public static SseResponseDto.InvitedNotificationResponseDto toInvitedNotification(WorkspaceInvitation invitation, Workspace workspace) {
        return SseResponseDto.InvitedNotificationResponseDto.builder()
                .eventType("INVITATION")
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getWorkspaceName())
                .invitedAt(invitation.getSentAt())
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
