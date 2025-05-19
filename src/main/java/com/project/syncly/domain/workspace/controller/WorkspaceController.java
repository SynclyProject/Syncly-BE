package com.project.syncly.domain.workspace.controller;

import com.project.syncly.domain.workspace.service.WorkspaceServiceImpl;
import com.project.syncly.domain.workspace.dto.WorkspaceRequestDto;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.CreateWorkspaceResponseDto response = workspaceService.createPersonalWorkspace(memberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, response));
    }

    // 팀 워크스페이스 생성 API
    @PostMapping("/team")
    @Operation(summary = "팀 워크 스페이스 생성 API")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.CreateWorkspaceResponseDto>> createTeamWorkspace(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @RequestBody @Valid WorkspaceRequestDto.CreateTeamWorkspaceRequestDto workspaceRequestDto
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.CreateWorkspaceResponseDto response = workspaceService.createTeamWorkspace(memberId, workspaceRequestDto.workspaceName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, response));
    }

    @PostMapping("/{workspaceId}/invite")
    @Operation(summary = "워크스페이스 초대 API")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.InviteWorkspaceResponseDto>> inviteToWorkspace(
            @PathVariable("workspaceId") Long workspaceId,
            @RequestBody @Valid WorkspaceRequestDto.CreateInvitationMailRequestDto workspaceRequestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long inviterId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.InviteWorkspaceResponseDto response = workspaceService.inviteTeamWorkspace(workspaceId, inviterId, workspaceRequestDto.email());

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }

    @GetMapping("/accept/{token}")
    @Operation(summary = "워크스페이스 초대 수락 API(이메일 링크 클릭)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.AcceptWorkspaceResponseDto>> acceptInvitationByToken(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable("token") String token
    ) {
        Long inviteeId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.AcceptWorkspaceResponseDto response = workspaceService.acceptInvitationByToken(inviteeId, token);

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }


    @PostMapping("/accept")
    @Operation(summary = "워크스페이스 초대 수락 API(알림창)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.AcceptWorkspaceResponseDto>> acceptInvitation(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @RequestBody @Valid WorkspaceRequestDto.acceptInvitationRequestDto workspaceRequestDto
    ) {
        Long inviteeId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.AcceptWorkspaceResponseDto response = workspaceService.acceptInvitation(inviteeId, workspaceRequestDto.invitationId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }

    @PostMapping("/reject")
    @Operation(summary = "워크스페이스 초대 거절 API(알림창)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.RejectWorkspaceResponseDto>> rejectInvitation(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @RequestBody @Valid WorkspaceRequestDto.rejectInvitationRequestDto workspaceRequestDto
    ) {
        Long inviteeId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.RejectWorkspaceResponseDto response = workspaceService.rejectInvitation(inviteeId, workspaceRequestDto.invitationId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(CustomResponse.success(HttpStatus.OK, response));
    }


    @GetMapping("/invites")
    @Operation(summary = "사용자의 초대 목록 조회")
    public ResponseEntity<CustomResponse<List<WorkspaceResponseDto.InvitationInfoDto>>> getInvitations(
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        List<WorkspaceResponseDto.InvitationInfoDto> invites = workspaceService.getInvitations(memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, invites));
    }




}
