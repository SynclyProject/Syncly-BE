package com.project.syncly.domain.url.converter;

import com.project.syncly.domain.url.dto.UrlHttpResponseDto;
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
                .action("TAB_ADD")
                .urlTabId(urlTab.getId())
                .workspaceId(urlTab.getWorkspace().getId())
                .urlTabName(urlTab.getTabName())
                .createdAt(urlTab.getCreatedAt())
                .build();
    }

    public static UrlWebSocketResponseDto.DeleteUrlTabResponseDto toDeleteUrlTabResponse(Long urlTabId, Long workspaceId) {
        return UrlWebSocketResponseDto.DeleteUrlTabResponseDto.builder()
                .action("TAB_DELETE")
                .urlTabId(urlTabId)
                .workspaceId(workspaceId)
                .deletedAt(LocalDateTime.now())
                .build();
    }

    public static UrlWebSocketResponseDto.UpdateUrlTabNameResponseDto toUpdateUrlTabNameResponse(UrlTab urlTab) {
        return UrlWebSocketResponseDto.UpdateUrlTabNameResponseDto.builder()
                .action("TAB_UPDATE")
                .urlTabId(urlTab.getId())
                .workspaceId(urlTab.getWorkspace().getId())
                .updatedTabName(urlTab.getTabName())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static UrlHttpResponseDto.CreateUrlTabResponseDto toUrlTabHttpResponse(UrlTab urlTab) {
        return UrlHttpResponseDto.CreateUrlTabResponseDto.builder()
                .action("TAB_ADD")
                .urlTabId(urlTab.getId())
                .workspaceId(urlTab.getWorkspace().getId())
                .urlTabName(urlTab.getTabName())
                .createdAt(urlTab.getCreatedAt())
                .build();
    }

    public static UrlHttpResponseDto.DeleteUrlTabResponseDto toDeleteUrlTabHttpResponse(Long urlTabId, Long workspaceId) {
        return UrlHttpResponseDto.DeleteUrlTabResponseDto.builder()
                .action("TAB_DELETE")
                .urlTabId(urlTabId)
                .workspaceId(workspaceId)
                .deletedAt(LocalDateTime.now())
                .build();
    }

    public static UrlHttpResponseDto.UpdateUrlTabNameResponseDto toUpdateUrlTabNameHttpResponse(UrlTab urlTab) {
        return UrlHttpResponseDto.UpdateUrlTabNameResponseDto.builder()
                .action("TAB_UPDATE")
                .urlTabId(urlTab.getId())
                .workspaceId(urlTab.getWorkspace().getId())
                .updatedTabName(urlTab.getTabName())
                .updatedAt(LocalDateTime.now())
                .build();
    }

}
