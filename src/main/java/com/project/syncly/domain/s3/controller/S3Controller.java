package com.project.syncly.domain.s3.controller;


import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;
import com.project.syncly.domain.s3.service.S3Service;
import com.project.syncly.global.anotations.MemberIdInfo;
import com.project.syncly.global.apiPayload.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    public ResponseEntity<CustomResponse<S3ResponseDTO.PreSignedUrl>> generatePresignedUrl(
            @RequestBody @Valid S3RequestDTO.PreSignedUrl request,
            @MemberIdInfo Long memberId) {
        S3ResponseDTO.PreSignedUrl presignedUrl = s3Service.generatePresignedUrl(memberId, request);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK ,presignedUrl));
    }
}
