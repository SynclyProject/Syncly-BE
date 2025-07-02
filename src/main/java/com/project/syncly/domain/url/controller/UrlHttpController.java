package com.project.syncly.domain.url.controller;

import com.project.syncly.domain.url.dto.UrlHttpResponseDto;
import com.project.syncly.domain.url.service.UrlHttpServiceImpl;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
@Tag(name = "URL 관련 API(해당 API는 http요청의 API로 웹소켓 관련 API는 별도 분리)")
public class UrlHttpController {

    private final UrlHttpServiceImpl urlHttpService;

    @GetMapping("/{workspaceId}/tabs-with-urls")
    @Operation(summary = "워크스페이스 내 모든 탭 및 URL 리스트 조회 (초기 렌더링 + 구독 대상)")
    public ResponseEntity<CustomResponse<UrlHttpResponseDto.TabsWithUrlsResponseDto>> getTabsWithUrls(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        UrlHttpResponseDto.TabsWithUrlsResponseDto response = urlHttpService.getTabsWithUrls(workspaceId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }
}
