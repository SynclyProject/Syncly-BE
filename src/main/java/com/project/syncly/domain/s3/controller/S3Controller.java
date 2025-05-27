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
        S3ResponseDTO.PreSignedUrl presignedUrl = s3Service.generatePresignedPutUrl(memberId, request);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK ,presignedUrl));
    }

    // 2. 이미지 조회용 Presigned URL 발급 (권한 없이 공개 이미지 사용 가능)
    @PostMapping("/view-url")
    public ResponseEntity<CustomResponse<S3ResponseDTO.GetUrl>> getViewUrl(@RequestBody @Valid S3RequestDTO.GetViewUrl request) {
        S3ResponseDTO.GetUrl url = s3Service.generatePresignedGetViewUrl(request);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, url));
    }


    // 3. 파일 다운로드용 Presigned URL 발급 (로그인 필요)
    @GetMapping("/download-url")
    public ResponseEntity<CustomResponse<S3ResponseDTO.GetUrl>> generatePresignedGetDownloadUrl(
            @RequestBody @Valid S3RequestDTO.GetDownloadUrl request,
            @MemberIdInfo Long memberId) {
        S3ResponseDTO.GetUrl url = s3Service.generatePresignedGetDownloadUrl(request, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, url));
    }

}
