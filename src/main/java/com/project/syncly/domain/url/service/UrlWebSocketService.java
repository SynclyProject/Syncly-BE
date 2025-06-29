package com.project.syncly.domain.url.service;

import com.project.syncly.domain.url.dto.UrlWebSocketRequestDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.domain.workspace.dto.WorkspaceMemberInfoResponseDto;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;

import java.util.List;

public interface UrlWebSocketService {
    public UrlWebSocketResponseDto.CreateUrlTabResponseDto createUrlTab(String userEmail, UrlWebSocketRequestDto.CreateUrlTabRequestDto request);
}

