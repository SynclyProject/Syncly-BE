package com.project.syncly.domain.url.controller;

import com.project.syncly.domain.url.dto.UrlHttpRequestDto;
import com.project.syncly.domain.url.dto.UrlHttpResponseDto;
import com.project.syncly.domain.url.dto.UrlWebSocketRequestDto;
import com.project.syncly.domain.url.dto.UrlWebSocketResponseDto;
import com.project.syncly.domain.url.service.UrlHttpServiceImpl;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
@Tag(name = "URL 관련 API(탭리스트 조회&개인워크스페이스)(해당 API는 http요청의 API로 웹소켓 관련 API는 별도 분리)")
public class UrlHttpController {
    private final UrlHttpServiceImpl urlHttpService;
    //refactor
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

    //URL 탭 생성 API
    @PostMapping("/{workspaceId}/tabs")
    @Operation(summary = "URL 탭 생성 API")
    public ResponseEntity<CustomResponse<UrlHttpResponseDto.CreateUrlTabResponseDto>> createUrlTab(
            @PathVariable Long workspaceId,
            @RequestBody @Valid UrlHttpRequestDto.CreateUrlTabRequestDto request,
            @AuthenticationPrincipal PrincipalDetails userDetails) {
        Long memberId = Long.valueOf(userDetails.getName());

        UrlHttpResponseDto.CreateUrlTabResponseDto response = urlHttpService.createUrlTab(memberId, workspaceId, request);

        // 생성 시
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomResponse.success(HttpStatus.CREATED, response));
    }

    //URL 탭 삭제 API
    @DeleteMapping("/{workspaceId}/tabs/{tabId}")
    @Operation(summary = "URL 탭 삭제 API")
    public ResponseEntity<CustomResponse<UrlHttpResponseDto.DeleteUrlTabResponseDto>> deleteUrlTab(
            @PathVariable Long workspaceId,
            @PathVariable Long tabId,
            @AuthenticationPrincipal PrincipalDetails userDetails) {
        Long memberId = Long.valueOf(userDetails.getName());

        UrlHttpResponseDto.DeleteUrlTabResponseDto response = urlHttpService.deleteUrlTab(memberId, workspaceId, tabId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    //URL 탭 이름 변경 API
    @PatchMapping("/{workspaceId}/tabs/{tabId}")
    @Operation(summary = "URL 탭 이름 변경 API")
    public ResponseEntity<CustomResponse<UrlHttpResponseDto.UpdateUrlTabNameResponseDto>> updateUrlTabName(
            @PathVariable Long workspaceId,
            @PathVariable Long tabId,
            @RequestBody @Valid UrlHttpRequestDto.UpdateUrlTabNameRequestDto request,
            @AuthenticationPrincipal PrincipalDetails userDetails) {
        Long memberId = Long.valueOf(userDetails.getName());

        UrlHttpResponseDto.UpdateUrlTabNameResponseDto response = urlHttpService.updateUrlTabName(memberId, workspaceId, tabId, request);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    //URL 아이템 추가 API
    @PostMapping("/tabs/{tabId}")
    @Operation(summary = "URL 아이템 추가 API")
    public ResponseEntity<CustomResponse<UrlHttpResponseDto.AddUrlItemResponseDto>> addUrlItem(
            @PathVariable Long tabId,
            @RequestBody @Valid UrlHttpRequestDto.AddUrlItemRequestDto request,
            @AuthenticationPrincipal PrincipalDetails userDetails) {
        Long memberId = Long.valueOf(userDetails.getName());

        UrlHttpResponseDto.AddUrlItemResponseDto response = urlHttpService.addUrlItem(memberId, tabId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomResponse.success(HttpStatus.CREATED, response));
    }

    // URL 아이템 삭제 API
    @DeleteMapping("/tabs/{tabId}/{itemId}")
    @Operation(summary = "URL 아이템 삭제 API")
    public ResponseEntity<CustomResponse<UrlHttpResponseDto.DeleteUrlItemResponseDto>> deleteUrlItem(
            @PathVariable Long tabId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal PrincipalDetails userDetails) {
        Long memberId = Long.valueOf(userDetails.getName());

        UrlHttpResponseDto.DeleteUrlItemResponseDto response = urlHttpService.deleteUrlItem(memberId, tabId, itemId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }


}
