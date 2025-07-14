package com.project.syncly.domain.folder.controller;

import com.project.syncly.domain.folder.dto.FolderRequestDto;
import com.project.syncly.domain.folder.dto.FolderResponseDto;
import com.project.syncly.domain.folder.service.FolderCommandService;
import com.project.syncly.global.anotations.MemberIdInfo;
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
@RequestMapping("/api/workspaces/{workspaceId}/folders")
@Tag(name = "Folder 관련 API")
public class FolderController {

    private final FolderCommandService folderCommandService;

    @PostMapping
    @Operation(summary = "폴더 생성 API")
    public ResponseEntity<CustomResponse<FolderResponseDto.Create>> createFolder(
            @PathVariable Long workspaceId,
            @MemberIdInfo Long memberId,
            @RequestBody @Valid FolderRequestDto.Create requestDto
    ) {
        FolderResponseDto.Create responseDto = folderCommandService.create(workspaceId, requestDto, memberId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomResponse.success(HttpStatus.CREATED, responseDto));
    }

}
