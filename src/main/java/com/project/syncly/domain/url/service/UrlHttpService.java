package com.project.syncly.domain.url.service;


import com.project.syncly.domain.url.dto.UrlHttpResponseDto;

public interface UrlHttpService {
    public UrlHttpResponseDto.TabsWithUrlsResponseDto getTabsWithUrls(Long workspaceId, Long memberId);
}

