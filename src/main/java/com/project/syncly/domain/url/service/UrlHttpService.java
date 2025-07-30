package com.project.syncly.domain.url.service;


import com.project.syncly.domain.url.dto.UrlHttpRequestDto;
import com.project.syncly.domain.url.dto.UrlHttpResponseDto;

public interface UrlHttpService {
    public UrlHttpResponseDto.TabsWithUrlsResponseDto getTabsWithUrls(Long workspaceId, Long memberId);
    public UrlHttpResponseDto.CreateUrlTabResponseDto createUrlTab(Long memberId, Long workspaceId, UrlHttpRequestDto.CreateUrlTabRequestDto request);
    public UrlHttpResponseDto.DeleteUrlTabResponseDto deleteUrlTab(Long memberId, Long workspaceId, Long tabId);
    public UrlHttpResponseDto.UpdateUrlTabNameResponseDto updateUrlTabName(Long memberId, Long workspaceId, Long tabId, UrlHttpRequestDto.UpdateUrlTabNameRequestDto request);
    public UrlHttpResponseDto.AddUrlItemResponseDto addUrlItem(Long memberId, Long tabId, UrlHttpRequestDto.AddUrlItemRequestDto request);
    public UrlHttpResponseDto.DeleteUrlItemResponseDto deleteUrlItem(Long memberId, Long tabId, Long itemId);
}

