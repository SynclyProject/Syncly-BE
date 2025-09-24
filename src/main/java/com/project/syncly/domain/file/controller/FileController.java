package com.project.syncly.domain.file.controller;

import com.project.syncly.domain.file.dto.FileRequestDto;
import com.project.syncly.domain.file.dto.FileResponseDto;
import com.project.syncly.domain.file.service.FileCommandService;
import com.project.syncly.domain.file.service.FileQueryService;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import com.project.syncly.global.apiPayload.CustomResponse;
import com.project.syncly.global.jwt.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/{workspaceId}/files/presigned-url")
    @Operation(summary = "파일 업로드 Presigned URL 발급", description = "워크스페이스에 파일을 업로드하기 위한 Presigned URL을 발급합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.PresignedUrl>> getFileUploadPresignedUrl(
            @PathVariable Long workspaceId,
            @RequestBody @Valid FileRequestDto.UploadPresignedUrl requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());
        FileResponseDto.PresignedUrl responseDto = fileCommandService.generatePresignedUrl(
                workspaceId, requestDto.folderId(), memberId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, responseDto));
    }

    @PostMapping("/{workspaceId}/files/confirm-upload")
    @Operation(summary = "파일 업로드 완료 확인", description = "Presigned URL로 업로드 완료된 파일을 DB에 저장합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Upload>> confirmFileUpload(
            @PathVariable Long workspaceId,
            @RequestBody @Valid FileRequestDto.ConfirmUpload requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());
        FileResponseDto.Upload responseDto = fileCommandService.confirmFileUpload(
                workspaceId, memberId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, responseDto));
    }

    @GetMapping("/{workspaceId}/files/{fileId}/download")
    @Operation(summary = "파일 다운로드 URL 생성", description = "워크스페이스의 파일에 대한 임시 다운로드 URL을 생성합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.DownloadUrl>> getFileDownloadUrl(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());
        FileResponseDto.DownloadUrl responseDto = fileQueryService.getFileDownloadUrl(workspaceId, fileId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @PatchMapping("/{workspaceId}/files/{fileId}")
    @Operation(summary = "파일 이름 변경", description = "워크스페이스의 파일 이름을 변경합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Update>> updateFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId,
            @RequestBody @Valid FileRequestDto.Update requestDto,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());
        FileResponseDto.Update responseDto = fileCommandService.updateFileName(
                workspaceId, fileId, memberId, requestDto);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @DeleteMapping("/{workspaceId}/files/{fileId}")
    @Operation(summary = "파일 휴지통 이동", description = "워크스페이스의 파일을 휴지통으로 이동시킵니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Message>> deleteFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());
        FileResponseDto.Message responseDto = fileCommandService.deleteFile(
                workspaceId, fileId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

    @PostMapping("/{workspaceId}/files/{fileId}/restore")
    @Operation(summary = "파일 복원", description = "워크스페이스의 휴지통 파일을 복원합니다.")
    public ResponseEntity<CustomResponse<FileResponseDto.Message>> restoreFile(
            @PathVariable Long workspaceId,
            @PathVariable Long fileId,
            @AuthenticationPrincipal PrincipalDetails userDetails
    ) {
        Long memberId = Long.valueOf(userDetails.getName());
        FileResponseDto.Message responseDto = fileCommandService.restoreFile(
                workspaceId, fileId, memberId);

        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, responseDto));
    }

}