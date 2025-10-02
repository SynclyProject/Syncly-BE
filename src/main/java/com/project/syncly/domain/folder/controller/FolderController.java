package com.project.syncly.domain.folder.controller;

import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.service.FolderQueryService;
import com.project.syncly.domain.folder.service.FolderCommandService;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
@Tag(name = "Folder & File API", description = "폴더 및 파일 관리 API")
public class FolderController {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final FolderQueryService folderQueryService;
    private final FolderCommandService folderCommandService;

    @PostMapping("/{workspaceId}/folders")
    @Operation(summary = "폴더 생성", description
        = "워크스페이스에서 새로운 폴더를 생성합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Create>> createFolder(
            @PathVariable Long workspaceId,
            @RequestBody @Valid FolderRequestDto.Create requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Create responseDto = folderCommandService.create(workspaceId, requestDto, currentMemberId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, responseDto));
    }

    @PatchMapping("/{workspaceId}/folders/{folderId}")
    @Operation(summary = "폴더 이름 변경", description = "워크스페이스의 폴더 이름을 변경합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Update>> updateFolder(
            @PathVariable Long workspaceId,
            @PathVariable Long folderId,
            @RequestBody @Valid FolderRequestDto.Update requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Update responseDto = folderCommandService.updateFolderName(workspaceId, folderId, requestDto, currentMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @DeleteMapping("/{workspaceId}/folders/{folderId}")
    @Operation(summary = "폴더 휴지통 이동", description = "워크스페이스의 폴더를 휴지통으로 이동시킵니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Message>> deleteFolder(
            @PathVariable Long workspaceId,
            @PathVariable Long folderId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Message responseDto = folderCommandService.deleteFolder(workspaceId, folderId, currentMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @PostMapping("/{workspaceId}/folders/{folderId}/restore")
    @Operation(summary = "폴더 복원", description = "워크스페이스의 휴지통 폴더를 복원합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Message>> restoreFolder(
            @PathVariable Long workspaceId,
            @PathVariable Long folderId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Message responseDto = folderCommandService.restoreFolder(workspaceId, folderId, currentMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/folders/{folderId}/path")
    @Operation(summary = "폴더 경로 조회", description = "워크스페이스 폴더의 breadcrumb 경로를 조회합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Path>> getFolderPath(
            @PathVariable Long workspaceId,
            @PathVariable Long folderId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Path responseDto = folderQueryService.getFolderPath(workspaceId, folderId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/root")
    @Operation(summary = "워크스페이스 루트 폴더 정보", description = "워크스페이스의 루트 폴더 ID와 기본 정보를 조회합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Root>> getWorkspaceRoot(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Root responseDto = folderQueryService.getRootFolder(workspaceId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/trash")
    @Operation(
        summary = "휴지통 조회",
        description = """
            워크스페이스의 휴지통에 있는 삭제된 폴더와 파일 목록을 조회합니다.

            **정렬 옵션:**
            - latest: 최신순 정렬 (삭제일시 기준 내림차순)
            - alphabet: 가나다순 정렬 (이름 기준 오름차순)

            **커서 기반 페이징:**
            - latest 정렬시: cursor는 날짜시간 형태 (예: "2025-09-16T23:59:59")
            - alphabet 정렬시: cursor는 이름 형태 (예: "Documents")

            **검색:**
            - 폴더명/파일명에서 부분 일치 검색 지원

            **업로더 필터링:**
            - uploaderId 파라미터로 특정 멤버가 삭제한 파일/폴더만 조회 가능
            - null이면 모든 업로더의 삭제된 파일/폴더 조회

            **응답 데이터:**
            - 삭제된 폴더와 파일이 통합되어 반환
            - 업로더 정보(이름, 프로필 이미지) 포함
            - 파일의 경우 크기, objectKey, 타입 정보 포함
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "휴지통 목록 조회 성공"),
        @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
        @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<FolderResponseDto.ItemList>> getTrashItems(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(
                description = "정렬 방식",
                example = "latest",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"latest", "alphabet"}
                )
            ) @RequestParam(defaultValue = "latest") String sort,
            @Parameter(
                description = "커서 (페이징용). latest 정렬시 날짜시간, alphabet 정렬시 이름",
                example = "2025-09-16T23:59:59"
            ) @RequestParam(required = false) String cursor,
            @Parameter(
                description = "한 페이지당 항목 수 (1-100)",
                example = "20"
            ) @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(
                description = "검색어 (폴더명/파일명 부분 일치)",
                example = "회의록"
            ) @RequestParam(required = false) String search,
            @Parameter(
                description = "업로더 ID (특정 멤버가 삭제한 파일/폴더만 조회)",
                example = "123"
            ) @RequestParam(required = false) Long uploaderId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.ItemList responseDto = folderQueryService.getTrashItems(workspaceId, sort, cursor, limit, search, uploaderId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/folders/{folderId}/items")
    @Operation(
        summary = "폴더/파일 목록 조회",
        description = """
            워크스페이스 폴더 내의 파일과 하위 폴더 목록을 조회합니다.

            **정렬 옵션:**
            - latest: 최신순 정렬 (수정일시 기준 내림차순)
            - alphabet: 가나다순 정렬 (이름 기준 오름차순)

            **커서 기반 페이징:**
            - latest 정렬시: cursor는 날짜시간 형태 (예: "2025-09-16T23:59:59")
            - alphabet 정렬시: cursor는 이름 형태 (예: "Documents")

            **검색:**
            - 폴더명/파일명에서 부분 일치 검색 지원

            **업로더 필터링:**
            - uploaderId 파라미터로 특정 멤버가 업로드한 파일/폴더만 조회 가능
            - null이면 모든 업로더의 파일/폴더 조회

            **응답 데이터:**
            - 폴더와 파일이 통합되어 반환
            - 업로더 정보(이름, 프로필 이미지) 포함
            - 파일의 경우 크기, objectKey, 타입 정보 포함
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "폴더/파일 목록 조회 성공"),
        @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
        @ApiResponse(responseCode = "404", description = "폴더를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<FolderResponseDto.ItemList>> getFolderItems(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "폴더 ID") @PathVariable Long folderId,
            @Parameter(
                description = "정렬 방식",
                example = "latest",
                schema = @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {"latest", "alphabet"}
                )
            ) @RequestParam(defaultValue = "latest") String sort,
            @Parameter(
                description = "커서 (페이징용). latest 정렬시 날짜시간, alphabet 정렬시 이름",
                example = "2025-09-16T23:59:59"
            ) @RequestParam(required = false) String cursor,
            @Parameter(
                description = "한 페이지당 항목 수 (1-100)",
                example = "20"
            ) @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(
                description = "검색어 (폴더명/파일명 부분 일치)",
                example = "회의록"
            ) @RequestParam(required = false) String search,
            @Parameter(
                description = "업로더 ID (특정 멤버가 업로드한 파일/폴더만 조회)",
                example = "123"
            ) @RequestParam(required = false) Long uploaderId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        // 워크스페이스 멤버십 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }
        FolderResponseDto.ItemList responseDto = folderQueryService.getFolderItems(workspaceId, folderId, sort, cursor, limit, search, uploaderId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @DeleteMapping("/{workspaceId}/folders/{folderId}/hard")
    @Operation(summary = "폴더 완전 삭제", description = "워크스페이스의 폴더를 완전히 삭제합니다. (복구 불가)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "폴더 완전 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "워크스페이스 멤버가 아님"),
        @ApiResponse(responseCode = "404", description = "폴더를 찾을 수 없음")
    })
    public ResponseEntity<CustomResponse<FolderResponseDto.Message>> hardDeleteFolder(
            @Parameter(description = "워크스페이스 ID") @PathVariable Long workspaceId,
            @Parameter(description = "폴더 ID") @PathVariable Long folderId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        FolderResponseDto.Message responseDto = folderCommandService.hardDeleteFolder(workspaceId, folderId, currentMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }
}