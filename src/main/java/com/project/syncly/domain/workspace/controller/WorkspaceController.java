package com.project.syncly.domain.workspace.controller;

import com.project.syncly.domain.workspace.dto.WorkspaceMemberInfoResponseDto;
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

    @PatchMapping("/{workspaceId}/name")
    @Operation(summary = "팀 워크스페이스 이름 변경 API (MANAGER만 가능)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.RenameWorkspaceResponseDto>> renameWorkspace(
            @PathVariable Long workspaceId,
            @RequestBody @Valid WorkspaceRequestDto.RenameWorkspaceRequestDto requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.RenameWorkspaceResponseDto response = workspaceService.renameTeamWorkspace(workspaceId, memberId, requestDto.newName());

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }


    @DeleteMapping("/{workspaceId}/leave")
    @Operation(summary = "워크스페이스 나가기 API (MANAGER가 나갈시에는 위임)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.LeaveWorkspaceResponseDto>> leaveWorkspace(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long workspaceId
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.LeaveWorkspaceResponseDto response = workspaceService.leaveWorkspace(workspaceId, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }


    @DeleteMapping("/{workspaceId}/members/{targetMemberId}/kick")
    @Operation(summary = "워크스페이스 멤버 추방 API (매니저만 가능/본인 추방 불가)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.KickMemberResponseDto>> kickMember(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long workspaceId,
            @PathVariable Long targetMemberId
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.KickMemberResponseDto response = workspaceService.kickMember(workspaceId, memberId, targetMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    @GetMapping()
    @Operation(summary = "내가 가입된 모든 워크스페이스 리스트 조회")
    public ResponseEntity<CustomResponse<List<WorkspaceResponseDto.MyWorkspaceResponseDto>>> getMyWorkspaces(
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        List<WorkspaceResponseDto.MyWorkspaceResponseDto> workspaces = workspaceService.getMyWorkspaces(memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, workspaces));
    }

    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "워크스페이스 소속 멤버 조회 API")
    public ResponseEntity<CustomResponse<List<WorkspaceMemberInfoResponseDto>>> getWorkspaceMembers(
            @PathVariable Long workspaceId
    ) {
        List<WorkspaceMemberInfoResponseDto> members = workspaceService.getWorkspaceMembers(workspaceId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, members));
    }

    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "워크스페이스 삭제 API (MANAGER 권한만 가능)")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.DeleteWorkspaceResponseDto>> deleteWorkspace(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long workspaceId
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.DeleteWorkspaceResponseDto response = workspaceService.deleteWorkspace(workspaceId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }

    @GetMapping("/{workspaceId}/role")
    @Operation(summary = "해당 워크스페이스 내 본인 ROLE 조회 API")
    public ResponseEntity<CustomResponse<WorkspaceResponseDto.GetWorkspaceRoleResponseDto>> getMyRole(
            @AuthenticationPrincipal PrincipalDetails userDetails,
            @PathVariable Long workspaceId
    ) {
        Long memberId = Long.valueOf(userDetails.getName());

        WorkspaceResponseDto.GetWorkspaceRoleResponseDto response = workspaceService.getMyRole(workspaceId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, response));
    }











}
