package com.project.syncly.domain.s3.service;

import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface S3Service {
    S3ResponseDTO.PreSignedUrl generatePresignedPutUrl(Long memberId, S3RequestDTO.UploadPreSignedUrl request);
    ResponseEntity<Void> generateSignedCookieForView(S3RequestDTO.GetViewUrl request, HttpServletResponse response);
    S3ResponseDTO.GetUrl generatePresignedGetDownloadUrl(S3RequestDTO.GetDownloadUrl request, Long memberId);

}
