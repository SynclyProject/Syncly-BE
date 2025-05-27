package com.project.syncly.domain.s3.service;

import com.project.syncly.domain.s3.dto.S3RequestDTO;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;

public interface S3Service {
    S3ResponseDTO.PreSignedUrl generatePresignedPutUrl(Long memberId, S3RequestDTO.PreSignedUrl request);
    S3ResponseDTO.GetUrl generatePresignedGetViewUrl(S3RequestDTO.GetViewUrl request);
    S3ResponseDTO.GetUrl generatePresignedGetDownloadUrl(S3RequestDTO.GetDownloadUrl request, Long memberId);

}
