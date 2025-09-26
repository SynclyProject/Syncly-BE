package com.project.syncly.domain.folder.controller;

import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.dto.ListingDto;
import com.project.syncly.domain.folder.dto.PermissionDto;
import com.project.syncly.domain.folder.service.FolderQueryService;
import com.project.syncly.domain.folder.service.FolderCommandService;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.domain.workspace.exception.WorkspaceErrorCode;
import com.project.syncly.domain.workspace.exception.WorkspaceException;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

        // 워크스페이스 멤버십 확인
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

        // 워크스페이스 멤버십 확인
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

        // 워크스페이스 멤버십 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        // 폴더 ID에 따른 다양한 경로 반환 (실제로는 DB에서 부모 폴더들을 재귀적으로 조회)
        List<FolderResponseDto.PathItem> pathItems;

        if (folderId.equals(1L)) {
            // 루트 폴더
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root")
            );
        } else if (folderId.equals(101L)) {
            // Documents 폴더
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(101L, "Documents")
            );
        } else if (folderId.equals(102L)) {
            // Images 폴더
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(102L, "Images")
            );
        } else if (folderId.equals(103L)) {
            // Archives 폴더
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(103L, "Archives")
            );
        } else if (folderId.equals(104L)) {
            // Backup 폴더
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(104L, "Backup")
            );
        } else if (folderId.equals(105L)) {
            // Resources 폴더
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(105L, "Resources")
            );
        } else if (folderId.equals(201L)) {
            // 깊은 폴더 구조 예시: Root > Documents > Projects > 2025
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(101L, "Documents"),
                    new FolderResponseDto.PathItem(200L, "Projects"),
                    new FolderResponseDto.PathItem(201L, "2025")
            );
        } else {
            // 기본적으로 루트 하위 폴더로 처리
            String folderName = "Folder_" + folderId;
            pathItems = List.of(
                    new FolderResponseDto.PathItem(1L, "Root"),
                    new FolderResponseDto.PathItem(folderId, folderName)
            );
        }

        FolderResponseDto.Path responseDto = new FolderResponseDto.Path(pathItems);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/root")
    @Operation(summary = "워크스페이스 루트 폴더 정보", description = "워크스페이스의 루트 폴더 ID와 기본 정보를 조회합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.Root>> getWorkspaceRoot(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        // 워크스페이스 멤버십 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        // 실제 루트 폴더 조회
        FolderResponseDto.Root responseDto = folderQueryService.getRootFolder(workspaceId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/trash")
    @Operation(summary = "휴지통 조회", description = "워크스페이스의 휴지통에 있는 폴더와 파일 목록을 조회합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.ItemList>> getTrashItems(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer limit,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        // 워크스페이스 멤버십 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }

        // TODO: 통합 휴지통 서비스 구현 필요
        // 1. 휴지통 파일 조회 (이미 구현됨)
        // 2. 휴지통 폴더 조회 (폴더 soft delete 구현 후)
        // 3. 두 결과를 ListingDto.Item 형태로 변환하여 통합

        // 현재는 빈 리스트와 더미 권한 반환
        List<ListingDto.Item> trashItems = java.util.Collections.emptyList();
        String nextCursor = null;

        // 현재 사용자의 워크스페이스 권한 정보 생성
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId);
        PermissionDto permissions = new PermissionDto(
                isMember, // canRead
                isMember, // canWrite
                isMember, // canDelete
                isMember  // canAdmin
        );

        FolderResponseDto.ItemList responseDto = new FolderResponseDto.ItemList(trashItems, nextCursor, permissions);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @GetMapping("/{workspaceId}/folders/{folderId}/items")
    @Operation(summary = "폴더/파일 목록 조회", description = "워크스페이스 폴더 내의 파일과 하위 폴더 목록을 조회합니다.")
    public ResponseEntity<CustomResponse<FolderResponseDto.ItemList>> getFolderItems(
            @PathVariable Long workspaceId,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long currentMemberId = Long.valueOf(userDetails.getName());

        // 워크스페이스 멤버십 확인
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId)) {
            throw new WorkspaceException(WorkspaceErrorCode.NOT_WORKSPACE_MEMBER);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        List<ListingDto.Item> items;

        if (search != null && !search.isEmpty()) {
            items = List.of(
                    new ListingDto.Item(
                            5002L,
                            "FILE",
                            "회의록.docx",
                            LocalDateTime.of(2025, 9, 15, 10, 0).format(formatter),
                            new ListingDto.UserInfo(12L, "이영희", "https://example.com/profile12.jpg")
                    ),
                    new ListingDto.Item(
                            5018L,
                            "FILE",
                            "회의록_0901.pdf",
                            LocalDateTime.of(2025, 9, 1, 14, 30).format(formatter),
                            new ListingDto.UserInfo(15L, "박회의", "https://example.com/profile15.jpg")
                    ),
                    new ListingDto.Item(
                            108L,
                            "FOLDER",
                            "회의자료",
                            LocalDateTime.of(2025, 8, 28, 16, 20).format(formatter),
                            new ListingDto.UserInfo(13L, "최회장", "https://example.com/profile13.jpg")
                    )
            );
        } else {
            items = List.of(
                    new ListingDto.Item(
                            5001L,
                            "FILE",
                            "design.png",
                            LocalDateTime.of(2025, 9, 17, 10, 0).format(formatter),
                            new ListingDto.UserInfo(10L, "홍길동", "https://example.com/profile10.jpg")
                    ),
                    new ListingDto.Item(
                            5002L,
                            "FILE",
                            "presentation.pptx",
                            LocalDateTime.of(2025, 9, 16, 15, 30).format(formatter),
                            new ListingDto.UserInfo(12L, "이영희", "https://example.com/profile12.jpg")
                    ),
                    new ListingDto.Item(
                            5003L,
                            "FILE",
                            "budget.xlsx",
                            LocalDateTime.of(2025, 9, 16, 9, 15).format(formatter),
                            new ListingDto.UserInfo(14L, "김예산", "https://example.com/profile14.jpg")
                    ),
                    new ListingDto.Item(
                            101L,
                            "FOLDER",
                            "Documents",
                            LocalDateTime.of(2025, 9, 15, 14, 0).format(formatter),
                            new ListingDto.UserInfo(11L, "김철수", "https://example.com/profile11.jpg")
                    ),
                    new ListingDto.Item(
                            5004L,
                            "FILE",
                            "logo_v2.svg",
                            LocalDateTime.of(2025, 9, 15, 11, 45).format(formatter),
                            new ListingDto.UserInfo(16L, "디자이너박", "https://example.com/profile16.jpg")
                    ),
                    new ListingDto.Item(
                            5005L,
                            "FILE",
                            "requirements.txt",
                            LocalDateTime.of(2025, 9, 14, 16, 20).format(formatter),
                            new ListingDto.UserInfo(17L, "개발자이", "https://example.com/profile17.jpg")
                    ),
                    new ListingDto.Item(
                            102L,
                            "FOLDER",
                            "Images",
                            LocalDateTime.of(2025, 9, 14, 13, 10).format(formatter),
                            new ListingDto.UserInfo(16L, "디자이너박", "https://example.com/profile16.jpg")
                    ),
                    new ListingDto.Item(
                            5006L,
                            "FILE",
                            "database_schema.sql",
                            LocalDateTime.of(2025, 9, 13, 10, 30).format(formatter),
                            new ListingDto.UserInfo(18L, "DB관리자정", "https://example.com/profile18.jpg")
                    ),
                    new ListingDto.Item(
                            5007L,
                            "FILE",
                            "user_manual.pdf",
                            LocalDateTime.of(2025, 9, 12, 14, 45).format(formatter),
                            new ListingDto.UserInfo(19L, "기술작가최", "https://example.com/profile19.jpg")
                    ),
                    new ListingDto.Item(
                            103L,
                            "FOLDER",
                            "Archives",
                            LocalDateTime.of(2025, 9, 11, 16, 0).format(formatter),
                            new ListingDto.UserInfo(11L, "김철수", "https://example.com/profile11.jpg")
                    ),
                    new ListingDto.Item(
                            5008L,
                            "FILE",
                            "api_documentation.md",
                            LocalDateTime.of(2025, 9, 10, 11, 20).format(formatter),
                            new ListingDto.UserInfo(20L, "문서화김", "https://example.com/profile20.jpg")
                    ),
                    new ListingDto.Item(
                            5009L,
                            "FILE",
                            "test_results.html",
                            LocalDateTime.of(2025, 9, 9, 15, 10).format(formatter),
                            new ListingDto.UserInfo(21L, "QA이", "https://example.com/profile21.jpg")
                    ),
                    new ListingDto.Item(
                            104L,
                            "FOLDER",
                            "Backup",
                            LocalDateTime.of(2025, 9, 8, 18, 30).format(formatter),
                            new ListingDto.UserInfo(18L, "DB관리자정", "https://example.com/profile18.jpg")
                    ),
                    new ListingDto.Item(
                            5010L,
                            "FILE",
                            "project_timeline.gantt",
                            LocalDateTime.of(2025, 9, 7, 9, 0).format(formatter),
                            new ListingDto.UserInfo(22L, "PM박", "https://example.com/profile22.jpg")
                    ),
                    new ListingDto.Item(
                            5011L,
                            "FILE",
                            "wireframe_v3.fig",
                            LocalDateTime.of(2025, 9, 6, 13, 40).format(formatter),
                            new ListingDto.UserInfo(23L, "UX디자이너한", "https://example.com/profile23.jpg")
                    ),
                    new ListingDto.Item(
                            105L,
                            "FOLDER",
                            "Resources",
                            LocalDateTime.of(2025, 9, 5, 10, 15).format(formatter),
                            new ListingDto.UserInfo(24L, "리소스매니저김", "https://example.com/profile24.jpg")
                    ),
                    new ListingDto.Item(
                            5012L,
                            "FILE",
                            "performance_report.xlsx",
                            LocalDateTime.of(2025, 9, 4, 14, 25).format(formatter),
                            new ListingDto.UserInfo(25L, "분석가이", "https://example.com/profile25.jpg")
                    ),
                    new ListingDto.Item(
                            5013L,
                            "FILE",
                            "security_audit.pdf",
                            LocalDateTime.of(2025, 9, 3, 16, 50).format(formatter),
                            new ListingDto.UserInfo(26L, "보안전문가최", "https://example.com/profile26.jpg")
                    ),
                    new ListingDto.Item(
                            106L,
                            "FOLDER",
                            "Legal",
                            LocalDateTime.of(2025, 9, 2, 11, 30).format(formatter),
                            new ListingDto.UserInfo(27L, "법무팀장박", "https://example.com/profile27.jpg")
                    ),
                    new ListingDto.Item(
                            5014L,
                            "FILE",
                            "deployment_guide.txt",
                            LocalDateTime.of(2025, 9, 1, 17, 10).format(formatter),
                            new ListingDto.UserInfo(28L, "데브옵스정", "https://example.com/profile28.jpg")
                    ),
                    new ListingDto.Item(
                            5015L,
                            "FILE",
                            "marketing_strategy.pptx",
                            LocalDateTime.of(2025, 8, 31, 12, 0).format(formatter),
                            new ListingDto.UserInfo(29L, "마케터김", "https://example.com/profile29.jpg")
                    ),
                    new ListingDto.Item(
                            107L,
                            "FOLDER",
                            "Templates",
                            LocalDateTime.of(2025, 8, 30, 15, 45).format(formatter),
                            new ListingDto.UserInfo(30L, "템플릿관리자", "https://example.com/profile30.jpg")
                    ),
                    new ListingDto.Item(
                            5016L,
                            "FILE",
                            "client_feedback.docx",
                            LocalDateTime.of(2025, 8, 29, 10, 20).format(formatter),
                            new ListingDto.UserInfo(31L, "CS팀장이", "https://example.com/profile31.jpg")
                    ),
                    new ListingDto.Item(
                            5017L,
                            "FILE",
                            "financial_report_q3.pdf",
                            LocalDateTime.of(2025, 8, 28, 16, 30).format(formatter),
                            new ListingDto.UserInfo(32L, "재무팀최", "https://example.com/profile32.jpg")
                    ),
                    new ListingDto.Item(
                            108L,
                            "FOLDER",
                            "Meetings",
                            LocalDateTime.of(2025, 8, 27, 9, 0).format(formatter),
                            new ListingDto.UserInfo(33L, "회의실관리자", "https://example.com/profile33.jpg")
                    ),
                    new ListingDto.Item(
                            5018L,
                            "FILE",
                            "competitor_analysis.xlsx",
                            LocalDateTime.of(2025, 8, 26, 14, 15).format(formatter),
                            new ListingDto.UserInfo(34L, "전략기획박", "https://example.com/profile34.jpg")
                    ),
                    new ListingDto.Item(
                            5019L,
                            "FILE",
                            "system_architecture.png",
                            LocalDateTime.of(2025, 8, 25, 11, 30).format(formatter),
                            new ListingDto.UserInfo(35L, "아키텍트정", "https://example.com/profile35.jpg")
                    ),
                    new ListingDto.Item(
                            109L,
                            "FOLDER",
                            "Training",
                            LocalDateTime.of(2025, 8, 24, 13, 45).format(formatter),
                            new ListingDto.UserInfo(36L, "교육담당자", "https://example.com/profile36.jpg")
                    ),
                    new ListingDto.Item(
                            5020L,
                            "FILE",
                            "privacy_policy.txt",
                            LocalDateTime.of(2025, 8, 23, 15, 0).format(formatter),
                            new ListingDto.UserInfo(37L, "개인정보보호담당", "https://example.com/profile37.jpg")
                    ),
                    new ListingDto.Item(
                            5021L,
                            "FILE",
                            "release_notes_v2.1.md",
                            LocalDateTime.of(2025, 8, 22, 10, 10).format(formatter),
                            new ListingDto.UserInfo(38L, "릴리즈매니저", "https://example.com/profile38.jpg")
                    ),
                    new ListingDto.Item(
                            110L,
                            "FOLDER",
                            "Vendors",
                            LocalDateTime.of(2025, 8, 21, 16, 20).format(formatter),
                            new ListingDto.UserInfo(39L, "구매담당자", "https://example.com/profile39.jpg")
                    )
            );
        }

        String nextCursor = "alphabet".equals(sort) ? "Docs" : "2025-09-16T23:59:59";

        // 현재 사용자의 워크스페이스 권한 정보 생성 (멤버라면 모든 권한 true)
        boolean isMember = workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, currentMemberId);
        PermissionDto permissions = new PermissionDto(
                isMember, // canRead
                isMember, // canWrite
                isMember, // canDelete
                isMember  // canAdmin
        );

        FolderResponseDto.ItemList responseDto = new FolderResponseDto.ItemList(items, nextCursor, permissions);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }
}