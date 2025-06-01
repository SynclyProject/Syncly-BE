package com.project.syncly.domain.s3.controller;


import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;
import com.project.syncly.domain.s3.service.S3Service;
import com.project.syncly.global.anotations.MemberIdInfo;
import com.project.syncly.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url/profile")
    public ResponseEntity<CustomResponse<S3ResponseDTO.PreSignedUrl>> getProfilePresignedUrl(
            @RequestBody @Valid S3RequestDTO.ProfileImageUploadPreSignedUrl request,
            @MemberIdInfo Long memberId) {

        return ResponseEntity.ok(CustomResponse.success(
                HttpStatus.OK, s3Service.generatePresignedPutUrl(memberId, request)));
    }

    @PostMapping("/presigned-url/drive")
    public ResponseEntity<CustomResponse<S3ResponseDTO.PreSignedUrl>> getDrivePresignedUrl(
            @RequestBody @Valid S3RequestDTO.DriveFileUploadPreSignedUrl request,
            @MemberIdInfo Long memberId) {
        return ResponseEntity.ok(CustomResponse.success(
                HttpStatus.OK, s3Service.generatePresignedPutUrl(memberId, request)));
    }

    // 이미지 조회용 CloudFront Signed Cookie 방식
    @PostMapping("/view-cookie")
    public ResponseEntity<Void> issueSignedCookieForView(
            @RequestBody @Valid S3RequestDTO.GetViewUrl request,
            HttpServletResponse response) {
        return s3Service.generateSignedCookieForView(request, response);
    }

    // 파일 다운로드용 Presigned URL 발급
    @PostMapping("/download-url")
    public ResponseEntity<CustomResponse<S3ResponseDTO.GetUrl>> generatePresignedGetDownloadUrl(
            @RequestBody @Valid S3RequestDTO.GetDownloadUrl request,
            @MemberIdInfo Long memberId) {
        S3ResponseDTO.GetUrl url = s3Service.generatePresignedGetDownloadUrl(request, memberId);
        return ResponseEntity.ok(CustomResponse.success(HttpStatus.OK, url));
    }

}
