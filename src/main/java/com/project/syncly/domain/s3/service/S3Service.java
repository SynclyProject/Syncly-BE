package com.project.syncly.domain.s3.service;

import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;

public interface S3Service {
    public S3ResponseDTO.PreSignedUrl generatePresignedUrl(Long memberId, S3RequestDTO.PreSignedUrl request);
}
