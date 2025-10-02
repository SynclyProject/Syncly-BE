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
    @Operation(
            summary = "파일 업로드 Presigned URL 발급",
            description = """
                    워크스페이스에 파일을 업로드하기 위한 S3 Presigned URL을 발급합니다.

                    **사용 방법:**
                    1. 이 API로 Presigned URL 발급
                    2. 발급받은 URL로 S3에 직접 파일 업로드 (PUT 요청)
                    3. /confirm-upload API로 업로드 완료 확인

                    **중요 사항:**
                    - **파일명에는 반드시 확장자가 포함되어야 합니다.**
                    - 예시: "보고서.pdf", "이미지.jpg", "동영상.mp4"
                    - 확장자 없이 입력하면 오류가 발생합니다.

                    **파일 제한사항:**
                    - 최대 크기: 200MB (209,715,200 바이트)
                    - 지원 형식: 이미지(jpg,png,gif,bmp,svg,webp), 동영상(mp4,avi,mkv,mov,wmv,flv,webm), 문서(pdf,doc,docx,xls,xlsx,ppt,pptx,txt)

                    **파일 크기 참고:**
                    - 1KB = 1,024 바이트
                    - 1MB = 1,048,576 바이트
                    - 10MB = 10,485,760 바이트
                    - 100MB = 104,857,600 바이트
                    """
    )
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
    @Operation(
            summary = "파일 업로드 완료 확인",
            description = """
                    Presigned URL로 업로드 완료된 파일을 DB에 저장합니다.

                    **중요 사항:**
                    - **파일명에는 반드시 확장자가 포함되어야 합니다.**
                    - Presigned URL 발급 시 사용한 파일명과 동일해야 합니다.
                    - 예시: "보고서.pdf", "이미지.jpg", "동영상.mp4"
                    - 확장자 없이 입력하면 오류가 발생합니다.

                    **사용 순서:**
                    1. Presigned URL 발급 (fileName: "파일.pdf")
                    2. S3에 파일 업로드
                    3. 이 API로 업로드 완료 확인 (fileName: "파일.pdf")
                    """
    )
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
    @Operation(
            summary = "파일 이름 변경",
            description = """
                    워크스페이스의 파일 이름을 변경합니다.

                    **중요 사항:**
                    - 파일명에는 **반드시 확장자가 포함**되어야 합니다.
                    - 예시: "보고서.pdf", "이미지.jpg", "문서.docx"
                    - 확장자 없이 입력하면 오류가 발생합니다.

                    **참고:**
                    - 실제 S3 파일은 변경되지 않고, 표시 이름만 변경됩니다.
                    - 같은 폴더 내에 동일한 이름의 파일이 있으면 오류가 발생합니다.
                    """
    )
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