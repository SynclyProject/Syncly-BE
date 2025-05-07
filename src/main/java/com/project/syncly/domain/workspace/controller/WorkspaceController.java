package com.project.syncly.domain.workspace.controller;

import com.project.syncly.domain.workspace.service.WorkspaceServiceImpl;
import com.project.syncly.domain.workspace.dto.WorkspaceRequestDto;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.global.apiPayload.CustomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceServiceImpl workspaceService;

    // 개인 워크스페이스 생성 API
    @PostMapping("/personal")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.CreateWorkspaceResponseDto>> createPersonalWorkspace(
            //@AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        //Long memberId = userDetails.getId();
        Long memberId = 1234L;

        WorkspaceResponseDto.CreateWorkspaceResponseDto response = workspaceService.createPersonalWorkspace(memberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, response));
    }

    // 팀 워크스페이스 생성 API
    @PostMapping("/team")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.CreateWorkspaceResponseDto>> createTeamWorkspace(
            //@AuthenticationPrincipal CustomUserDetails userDetails
            @RequestBody @Valid WorkspaceRequestDto.CreateTeamWorkspaceRequestDto workspaceRequestDto
    ) {
        //Long memberId = userDetails.getId();
        Long memberId = 1234L;

        WorkspaceResponseDto.CreateWorkspaceResponseDto response = workspaceService.createTeamWorkspace(memberId, workspaceRequestDto.workspaceName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, response));
    }
}
