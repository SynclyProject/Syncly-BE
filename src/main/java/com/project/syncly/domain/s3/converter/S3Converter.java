package com.project.syncly.domain.s3.converter;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.entity.SocialLoginProvider;
import com.project.syncly.domain.s3.dto.S3ResponseDTO;

public class S3Converter {
    public static S3ResponseDTO.PreSignedUrl toPreSignedUrlResDTO(String fileName, String url, String objectKey) {
        return S3ResponseDTO.PreSignedUrl.builder()
                .fileName(fileName)
                .uploadUrl(url)
                .objectKey(objectKey)
                .build();
    }
}
