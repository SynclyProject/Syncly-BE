package com.project.syncly.domain.workspace.controller;

import com.project.syncly.domain.workspace.service.WorkspaceServiceImpl;
import com.project.syncly.domain.workspace.dto.WorkspaceRequestDto;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
@Tag(name = "WorkSpace 관련 API")
public class WorkspaceController {

    private final WorkspaceServiceImpl workspaceService;

    // 개인 워크스페이스 생성 API
    @PostMapping("/personal")
    @Operation(summary = "개인 워크 스페이스 생성 API")
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
    @Operation(summary = "팀 워크 스페이스 생성 API")
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

    @PostMapping("/{workspaceId}/invite")
    @Operation(summary = "워크스페이스 초대 API")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.InviteWorkspaceResponseDto>> inviteToWorkspace(
            @PathVariable("workspaceId") Long workspaceId,
            @RequestBody @Valid WorkspaceRequestDto.CreateInvitationMailRequestDto workspaceRequestDto
    ) {
        // Long inviterId = userDetails.getId(); // 실제 로그인 정보 사용 시
        Long inviterId = 1234L; // 목 데이터

        WorkspaceResponseDto.InviteWorkspaceResponseDto response = workspaceService.inviteTeamWorkspace(workspaceId, inviterId, workspaceRequestDto.email());

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }

    @GetMapping("/accept/{token}")
    @Operation(summary = "워크스페이스 초대 수락 API(이메일 링크 클릭)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.AcceptWorkspaceResponseDto>> acceptInvitationByToken(
            @PathVariable("token") String token
    ) {
        // Long inviterId = userDetails.getId(); // 실제 로그인 정보 사용 시
        Long inviteeId = 2L; // 목 데이터

        WorkspaceResponseDto.AcceptWorkspaceResponseDto response = workspaceService.acceptInvitationByToken(inviteeId, token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }


    @PostMapping("/accept")
    @Operation(summary = "워크스페이스 초대 수락 API(알림창)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.AcceptWorkspaceResponseDto>> acceptInvitation(
            @RequestBody @Valid WorkspaceRequestDto.acceptInvitationRequestDto workspaceRequestDto
    ) {
        // Long inviterId = userDetails.getId(); // 실제 로그인 정보 사용 시
        Long inviteeId = 2L; // 목 데이터

        WorkspaceResponseDto.AcceptWorkspaceResponseDto response = workspaceService.acceptInvitation(inviteeId, workspaceRequestDto.invitationId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }

    @PostMapping("/reject")
    @Operation(summary = "워크스페이스 초대 거절 API(알림창)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.RejectWorkspaceResponseDto>> rejectInvitation(
            @RequestBody @Valid WorkspaceRequestDto.rejectInvitationRequestDto workspaceRequestDto
    ) {
        // Long inviterId = userDetails.getId(); // 실제 로그인 정보 사용 시
        Long inviteeId = 2L; // 목 데이터

        WorkspaceResponseDto.RejectWorkspaceResponseDto response = workspaceService.rejectInvitation(inviteeId, workspaceRequestDto.invitationId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }



}
