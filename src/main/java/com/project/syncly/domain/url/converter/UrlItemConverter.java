package com.project.syncly.domain.url.converter;

import com.project.syncly.domain.url.dto.UrlHttpResponseDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.domain.url.entity.UrlItem;
import com.project.syncly.domain.url.entity.UrlTab;

import java.time.LocalDateTime;
import java.util.List;


public class UrlItemConverter {

    public static UrlItem toUrlItem(UrlTab urlTab, String url) {
        return UrlItem.builder()
                .urlTab(urlTab)
                .url(url)
                .build();
    }

    public static UrlWebSocketResponseDto.AddUrlItemResponseDto toAddUrlItemResponse(UrlItem urlItem) {
        return UrlWebSocketResponseDto.AddUrlItemResponseDto.builder()
                .action("URL_ADD")
                .urlTabId(urlItem.getUrlTab().getId())
                .urlItemId(urlItem.getId())
                .url(urlItem.getUrl())
                .createdAt(urlItem.getCreatedAt())
                .build();
    }

    public static UrlWebSocketResponseDto.DeleteUrlItemResponseDto toDeleteUrlItemResponse(UrlItem urlItem) {
        return UrlWebSocketResponseDto.DeleteUrlItemResponseDto.builder()
                .action("URL_DELETE")
                .urlTabId(urlItem.getUrlTab().getId())
                .urlItemId(urlItem.getId())
                .deletedAt(LocalDateTime.now())
                .build();
    }

    public static UrlHttpResponseDto.TabsWithUrlsResponseDto toTabsWithUrlsResponse(
            Long workspaceId, List<UrlTab> urlTabs) {

        List<UrlHttpResponseDto.TabWithUrls> tabDtos = urlTabs.stream()
                .map(tab -> UrlHttpResponseDto.TabWithUrls.builder()
                        .tabId(tab.getId())
                        .tabName(tab.getTabName())
                        .createdAt(tab.getCreatedAt())
                        .urls(tab.getUrlItems().stream()
                                .map(item -> UrlHttpResponseDto.UrlItemInfo.builder()
                                        .urlItemId(item.getId())
                                        .url(item.getUrl())
                                        .createdAt(item.getCreatedAt())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return UrlHttpResponseDto.TabsWithUrlsResponseDto.builder()
                .workspaceId(workspaceId)
                .tabs(tabDtos)
                .build();
    }

    public static UrlHttpResponseDto.AddUrlItemResponseDto toAddUrlItemHttpResponse(UrlItem urlItem) {
        return UrlHttpResponseDto.AddUrlItemResponseDto.builder()
                .action("URL_ADD")
                .urlTabId(urlItem.getUrlTab().getId())
                .urlItemId(urlItem.getId())
                .url(urlItem.getUrl())
                .createdAt(urlItem.getCreatedAt())
                .build();
    }

    public static UrlHttpResponseDto.DeleteUrlItemResponseDto toDeleteUrlItemHttpResponse(UrlItem urlItem) {
        return UrlHttpResponseDto.DeleteUrlItemResponseDto.builder()
                .action("URL_DELETE")
                .urlTabId(urlItem.getUrlTab().getId())
                .urlItemId(urlItem.getId())
                .deletedAt(LocalDateTime.now())
                .build();
    }


}
