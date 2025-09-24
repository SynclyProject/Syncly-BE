package com.project.syncly.domain.file.controller;

import com.project.syncly.domain.file.dto.FileRequestDto;
import com.project.syncly.domain.file.dto.FileResponseDto;
import com.project.syncly.domain.file.service.FileCommandService;
import com.project.syncly.domain.file.service.FileQueryService;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
@Tag(name = "File API", description = "파일 관리 API")
public class FileController {

    private final FileCommandService fileCommandService;
    private final FileQueryService fileQueryService;

    @PostMapping(value = "/{workspaceId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "파일 업로드", description = "워크스페이스에 새로운 파일을 업로드합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Upload>> uploadFile(
            @PathVariable Long workspaceId,
            @Parameter(description = "업로드할 파일", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "폴더 ID", required = true)
            @RequestParam("folderId") Long folderId
    ) {
        // TODO: 현재 로그인한 사용자 ID 가져오기 (Spring Security에서)
        Long currentMemberId = 1L; // 임시값

        FileResponseDto.Upload responseDto = fileCommandService.uploadFile(
                workspaceId, folderId, currentMemberId, file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, responseDto));
    }

    @GetMapping("/{workspaceId}/files/{fileId}/download")
    @Operation(summary = "파일 다운로드", description = "워크스페이스의 파일을 다운로드합니다.")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId
    ) {
        // TODO: 현재 로그인한 사용자 ID 가져오기 (Spring Security에서)
        Long currentMemberId = 1L; // 임시값

        ByteArrayResource resource = fileQueryService.downloadFile(workspaceId, fileId, currentMemberId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample-file.txt\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PatchMapping("/{workspaceId}/files/{fileId}")
    @Operation(summary = "파일 이름 변경", description = "워크스페이스의 파일 이름을 변경합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Update>> updateFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId,
            @RequestBody @Valid FileRequestDto.Update requestDto
    ) {
        // TODO: 현재 로그인한 사용자 ID 가져오기 (Spring Security에서)
        Long currentMemberId = 1L; // 임시값

        FileResponseDto.Update responseDto = fileCommandService.updateFileName(
                workspaceId, fileId, currentMemberId, requestDto);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @DeleteMapping("/{workspaceId}/files/{fileId}")
    @Operation(summary = "파일 휴지통 이동", description = "워크스페이스의 파일을 휴지통으로 이동시킵니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Message>> deleteFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId
    ) {
        // TODO: 현재 로그인한 사용자 ID 가져오기 (Spring Security에서)
        Long currentMemberId = 1L; // 임시값

        FileResponseDto.Message responseDto = fileCommandService.deleteFile(
                workspaceId, fileId, currentMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @PostMapping("/{workspaceId}/files/{fileId}/restore")
    @Operation(summary = "파일 복원", description = "워크스페이스의 휴지통 파일을 복원합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Message>> restoreFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId
    ) {
        // TODO: 현재 로그인한 사용자 ID 가져오기 (Spring Security에서)
        Long currentMemberId = 1L; // 임시값

        FileResponseDto.Message responseDto = fileCommandService.restoreFile(
                workspaceId, fileId, currentMemberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

}