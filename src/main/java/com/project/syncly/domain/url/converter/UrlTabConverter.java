package com.project.syncly.domain.url.converter;

import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.domain.url.entity.UrlTab;
import com.project.syncly.domain.workspace.entity.Workspace;

import java.time.LocalDateTime;

public class UrlTabConverter {

    public static UrlTab toUrlTab(Workspace workspace, String urlTabName) {
        return UrlTab.builder()
                .workspace(workspace)
                .tabName(urlTabName)
                .build();
    }


    public static UrlWebSocketResponseDto.CreateUrlTabResponseDto toUrlTabResponse(UrlTab urlTab) {
        return UrlWebSocketResponseDto.CreateUrlTabResponseDto.builder()
                .urlTabId(urlTab.getId())
                .workspaceId(urlTab.getWorkspace().getId())
                .urlTabName(urlTab.getTabName())
                .createdAt(urlTab.getCreatedAt())
                .build();
    }

}
